# 视频上传链路（现状基线）

> 本文档描述**当前生产代码中**的视频上传完整链路，作为后续异步化改造的基线参考。
> 文档结论基于 trace `c8cb15ae1777997086754014de5d6c`（2026-05-06 00:04:46 ~ 00:05:37）的实测数据 + 代码走读。
>
> **核心结论**（用户改造前必读）：
> 1. `complete` 接口是**同步**的，单次响应时间 = 合并 + 探测 + OSS 上传 + **LLM 调用** + DB 写入。
> 2. **LLM 调用（Qwen）是当前 `complete` 同步链路的强依赖**，单次耗时 30s~60s+，是整条链路最大、最不稳定的耗时来源。LLM 失败 = 整个 `complete` 接口失败。
> 3. 前端 axios 全局 `timeout = 30000ms`，已**无法覆盖**实测的 ~50s 后端耗时，会触发"网络连接失败"假阳性（trace `c8cb...5d6c` 即为此现象）。
> 4. 视频在 DB 中的最终状态推进路径：`UPLOADED`（视频入库时）→ `METADATA_GENERATED`（LLM 成功后），两步**全在同一个 `complete` HTTP 请求里同步完成**。

---

## 1. 总览

### 1.1 上传协议：分片三段式

| 阶段 | 接口 | HTTP | 调用方 | 用途 |
|---|---|---|---|---|
| ① init | `/api/videos/upload/server/init` | POST | 前端 | 创建上传会话、分配 `uploadId`、约定分片大小 |
| ② chunk | `/api/videos/upload/server/{uploadId}/chunk` | POST (multipart) | 前端循环 | 逐个上传分片到服务器临时目录 |
| ③ complete | `/api/videos/upload/server/{uploadId}/complete` | POST | 前端 | 合并 → 探测 → OSS → 入库 → **LLM 元数据生成** → 状态推进 |

> 注：业务上还存在一个**异步事件** `VideoUploadedEvent`，由 `complete` 末尾发布；但**它不影响 `complete` 的同步返回时间**（详见第 4 节）。

### 1.2 整体时序

```
前端                                  后端 (8000)                                                Qwen LLM
 │                                          │                                                       │
 ├── POST /init ─────────────────────────►  │ INSERT upload_session (ACTIVE)                        │
 │ ◄── 200 { uploadId, totalChunks, ... }   │                                                       │
 │                                          │                                                       │
 ├── POST /{uploadId}/chunk (idx=0) ─────►  │ Files.copy → ./data/temp/{uploadId}/chunk_0           │
 │                                          │ UPDATE upload_session.uploadedChunks                  │
 │ ◄── 200                                  │                                                       │
 │  ...（共 totalChunks 次，前端单分片失败最多重试 3 次）                                              │
 │                                          │                                                       │
 ├── POST /{uploadId}/complete ──────────►  │ ① mergeChunks (FileChannel)                          │
 │                                          │ ② ffprobe inspect (~170ms)                           │
 │   ⏳ 同步等待 ~50s                         │ ③ OSS putObject (~2s/1.3MB)                          │
 │   ⚠ axios timeout = 30s                   │ ④ INSERT video (status=UPLOADED)                     │
 │   → 30s 后触发"网络连接失败"                │ ⑤ UPDATE upload_session (COMPLETED)                  │
 │                                          │ ⑥ cleanup local temp                                 │
 │                                          │ ⑦ publish VideoUploadedEvent ─┐                      │
 │                                          │                               │ Spring 同步派发        │
 │                                          │                               ▼                      │
 │                                          │   VideoUploadedEventListener.handle()                │
 │                                          │     → MetadataApplicationService.generateMetadata    │
 │                                          │       → metadataGenerationService.generate           │
 │                                          │         → QwenLlmServiceAdapter.complete ──POST──►  LLM (~47s)
 │                                          │                                              ◄──────│
 │                                          │       → INSERT video_metadata                        │
 │                                          │       → video.transitionTo(METADATA_GENERATED)       │
 │                                          │       → UPDATE video.status                          │
 │                                          │ ⑧ 事务提交                                            │
 │ ◄── 200 (后端返回了，但前端已 abort)       │ return ServerUploadCompleteDTO                       │
 │                                          │                                                       │
 │  之后前端继续轮询 GET /videos/{id}（最多 60 次 × 2s）等待 status ≠ UPLOADED                        │
```

**关键事实（由代码核实，非推断）**：
- ⑦ 的 `eventPublisher.publish` 走 `SpringDomainEventPublisher → ApplicationEventPublisher.publishEvent`，**Spring 默认同步派发**。
- 监听器用 `@EventListener`（**非** `@TransactionalEventListener`），所以监听器内的 INSERT/UPDATE 与外层 `completeUpload` **共用同一个事务**。
- ⑧ 事务提交一次性把 `video INSERT` + `upload_session UPDATE` + `video_metadata INSERT` + `video status UPDATE` 全部落盘。LLM 抛异常 → **整个事务回滚**，但 OSS 文件已经落地（OSS 不在事务内）。

---

## 2. 接口定义

代码位置：`grace-platform/src/main/java/com/grace/platform/video/interfaces/rest/ServerUploadController.java`
基础路径：`/api/videos/upload/server`

### 2.1 `POST /init`

**作用**：建会话、分配 `uploadId`、确定分片大小与总分片数。

**请求体**（`ServerUploadInitRequest`）：

```json
{
  "fileName": "A01京东送货正常光照外卖送达拍照离开.mp4",
  "fileSize": 1321275,
  "format": "MP4"
}
```

**响应体**（`ServerUploadInitResponse`）：

```json
{
  "uploadId": "upl_b766f359f49c4aaf",
  "totalChunks": 1,
  "chunkSize": 16777216,
  "tempDirectory": "./data/temp/upl_b766f359f49c4aaf",
  "expiresAt": "2026-05-07T00:04:47"
}
```

**关键校验**（在 `ServerUploadApplicationService.initUpload` 内）：
- `format` 必须在 `Video.SUPPORTED_FORMATS`（MP4 / MOV / AVI / MKV）；
- `fileSize` ≤ `Video.MAX_FILE_SIZE`（5GB）；
- 服务端 `Files.createDirectories(./data/temp/{uploadId})`，失败则抛 `INTERNAL_SERVER_ERROR`；
- 写 `upload_session` 表，状态 `ACTIVE`，过期时间 = 创建时间 + 24h。

### 2.2 `POST /{uploadId}/chunk`

**Content-Type**：`multipart/form-data`
**表单字段**：
- `chunkIndex`（int，0 起始）
- `chunk`（二进制分片）

**响应体**：

```json
{
  "uploadId": "upl_b766f359f49c4aaf",
  "chunkIndex": 0,
  "uploadedChunks": 1,
  "totalChunks": 1
}
```

**关键校验**（在 `uploadChunk` 内）：
- `session` 不存在 / 已过期 / 状态非 `ACTIVE` → 业务异常；
- `chunkIndex` 越界 → `CHUNK_INDEX_OUT_OF_RANGE`；
- 同一 chunk 重传 → `DUPLICATE_CHUNK`（通过 `Files.exists(chunk_{i})` 判断）；
- 分片落盘到 `./data/temp/{uploadId}/chunk_{idx}`；
- `session.uploadedChunks++`，写回 DB。

### 2.3 `POST /{uploadId}/complete`

**请求体**：空（仅靠路径上的 `uploadId`）。

**响应体**（`ServerUploadCompleteResponse`）：

```json
{
  "videoId": "vid_a7bba5267702486d90eae19e6163c57d",
  "fileName": "...mp4",
  "fileSize": 1321275,
  "format": "MP4",
  "duration": "PT16S",
  "status": "METADATA_GENERATED",
  "storageUrl": "https://xyf-kitchen-repo.oss-ap-southeast-1.aliyuncs.com/videos/20260506/upl_b766f359f49c4aaf_744599de.mp4",
  "createdAt": "2026-05-06T00:04:49.129"
}
```

**这是整条链路的耗时大头**，详见第 3.3 / 第 4 节。


## 3. 后端实现细节

### 3.1 类与职责

| 类 | 职责 |
|---|---|
| `ServerUploadController` | REST 入口，做 DTO ↔ Command 转换 |
| `ServerUploadApplicationService` | **同步事务编排**（`@Service @Transactional`），整条 `complete` 流程的总指挥 |
| `ChunkMergeServiceImpl` | 用 `FileChannel` 顺序合并 `chunk_0..chunk_N-1` 为最终文件，合并后立即删除分片 |
| `VideoFileInspectorImpl` | 调 `ProcessBuilder("ffprobe", ...)` 抓 duration；超时 30s |
| `AliOssStorageServiceImpl` | 阿里 OSS SDK `putObject`，同步阻塞等 OSS 完成 |
| `MetadataGenerationServiceImpl` | 拼 prompt + 调 `LlmService` + 解析 JSON（含 markdown / 正则三段兜底解析） |
| `QwenLlmServiceAdapter` | Qwen `chat/completions` 同步 HTTP 调用，最多 3 次 attempt，失败间 `Thread.sleep(1000ms / 2000ms)`（详见 4.3） |
| `VideoRepository` / `UploadSessionRepository` | MyBatis 持久化 |
| `DomainEventPublisher` | 发 `VideoUploadedEvent`（**与 `complete` 同步返回值无关**，详见 4.4） |

### 3.2 `init` / `chunk` 流程要点

- **`init`** 是一次性快接口，纯本地文件 + DB 写，无外部依赖，毫秒级。
- **`chunk`** 每次都做完整的 `findById(uploadId)` + `Files.copy(chunk)` + `incrementUploadedChunks` + `save(session)`，所以单分片有一次 DB 读 + 一次 DB 写 + 一次磁盘写。**每个分片独立请求**，不会被合并。
- 分片落盘使用 `Files.copy(stream, chunk_{idx}, REPLACE_EXISTING)`，文件命名为 `chunk_0`、`chunk_1`、…，**索引就是顺序号**，合并阶段直接按这个顺序拼接。

### 3.3 `complete` 同步流程详解（关键）

入口：`ServerUploadApplicationService.completeUpload(uploadId)`，整个方法**包在一个数据库事务里**（类上 `@Transactional`）。

```text
completeUpload(uploadId)
├─ ① uploadSessionRepository.findById  ── 校验 session 存在 / 未过期 / ACTIVE / chunks 全到
├─ ② chunkMergeService.mergeChunks     ── 本地磁盘 IO，FileChannel 拼接，毫秒~秒级
├─ ③ videoFileInspector.inspect        ── fork ffprobe 进程拿 duration，秒级，超时 30s
├─ ④ ossStorageService.uploadFile      ── 同步阻塞 OSS putObject，依赖文件大小 + 网络
├─ ⑤ ossStorageService.buildObjectUrl  ── 纯字符串拼接
├─ ⑥ Video.createWithStorageUrl        ── 构造聚合根，status = UPLOADED
├─ ⑦ videoRepository.save(video)       ── INSERT video（详见 6.1 字段）
├─ ⑧ session.markAsCompleted + save    ── upload_session.status = COMPLETED
├─ ⑨ cleanupTempDirectory              ── 删除合并后的文件 + 临时目录
├─ ⑩ eventPublisher.publish(VideoUploadedEvent)   ── 见 4.4，**不影响响应耗时**
└─ return ServerUploadCompleteDTO(...)
```

**注意**：上述 ① ~ ⑩ **全部在调用线程同步执行**，HTTP 响应必须等到 `return` 才发出。

但是！`status = UPLOADED` 在 ⑥/⑦ 就已经写到 video 表，**接下来从 `UPLOADED` 推进到 `METADATA_GENERATED` 的逻辑并不在 `ServerUploadApplicationService` 里**，而是在第 4 节描述的 `VideoUploadedEvent` 监听器内 —— 但**这个监听器是同步执行的**，所以最终 `complete` 接口返回时拿到的是 `METADATA_GENERATED`。这一点见 4.4，是异步化改造时最容易踩的坑。

### 3.4 事务边界

- `ServerUploadApplicationService` 类上有 `@Transactional`，所以 `completeUpload` 内的 **所有 DB 写**（INSERT video、UPDATE upload_session）都在**同一个事务**里。
- **OSS putObject、ffprobe、LLM 调用都不是事务感知的外部调用**，但它们都跑在事务持有期内 —— 这意味着事务**整整持有了 ~50s**，直到 LLM 返回、最后一个 UPDATE 提交才释放。这是异步化改造前另一个潜在坑：长事务会占着数据库连接。

### 3.5 错误处理

- `MetadataGenerationServiceImpl` 内部抛任何异常 → 由于 `@Transactional` 默认回滚 RuntimeException → **整个 complete 事务回滚**，video / video_metadata 都不会落表，但 **OSS 文件已经上传成功且不会被清理**（OSS putObject 不在事务内，没有补偿逻辑）。
- 客户端实测最常见的失败原因：LLM 超时 / `ExternalServiceException(LLM_SERVICE_UNAVAILABLE, 9001)`。


## 4. LLM 调用：当前链路的强依赖

> 这一节是用户做异步化改造前**最需要看清楚**的事实。

### 4.1 调用链路（每个箭头都对应一个具体类，全部由代码核实）

```
ServerUploadApplicationService.completeUpload
  └─ DomainEventPublisher.publish(VideoUploadedEvent)
       │  实现类：shared.infrastructure.event.SpringDomainEventPublisher
       │  实质：ApplicationEventPublisher.publishEvent(event)  ← Spring 默认同步派发
       ▼
     metadata.application.listener.VideoUploadedEventListener.handle(event)
       │  注解：@EventListener （非 @TransactionalEventListener，所以共用外层事务）
       ▼
     MetadataApplicationService.generateMetadata(videoId)
       │  类上 @Transactional，propagation=REQUIRED → 加入外层事务
       ├─ videoRepository.findById
       ├─ videoMetadataRepository.findByVideoId  (查历史风格参考)
       ├─ MetadataGenerationServiceImpl.generate(videoId, videoInfo, history)
       │    └─ QwenLlmServiceAdapter.complete(LlmRequest)
       │         └─ RestTemplate.exchange(POST https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions)
       ├─ videoMetadataRepository.insert(metadata)
       ├─ video.transitionTo(VideoStatus.METADATA_GENERATED)   ← 状态机校验：仅允许 UPLOADED → METADATA_GENERATED
       └─ videoRepository.save(video)
```

**为什么 `complete` 接口实测耗时会被 LLM 拖到 ~50s？**

1. `SpringDomainEventPublisher.publish` 调用 `ApplicationEventPublisher.publishEvent`，**Spring 默认是同步派发**（要异步必须加 `@Async` 或自定义 `ApplicationEventMulticaster`，本仓库都没有）。
2. `VideoUploadedEventListener.handle` 用的是 `@EventListener` 而**不是** `@TransactionalEventListener(phase=AFTER_COMMIT)`，所以监听器**直接在发布线程上同步跑**，且**复用外层事务**。
3. `MetadataApplicationService` 类上有 `@Transactional`，按 Spring 默认 `REQUIRED` 传播策略，会**加入**外层 `ServerUploadApplicationService.completeUpload` 的事务，不会另起一个。
4. 综上：LLM 请求耗时 = `eventPublisher.publish` 阻塞耗时 = `completeUpload` 阻塞耗时 = HTTP 响应等待耗时。

### 4.2 实测耗时（trace `c8cb15ae...`）

| 步骤 | 起止时刻 | 耗时 |
|---|---|---|
| ② 合并分片 | 00:04:46.780 → .788 | 8ms |
| ③ ffprobe 探测 | (.788 → .956 包含构造) | ~170ms |
| ④ OSS putObject | 00:04:46.956 → 00:04:49.127 | **2.17s** |
| ⑦ INSERT video | 00:04:49.129 → .157 | 28ms |
| ⑧ UPDATE upload_session | 00:04:49.172 → .197 | 25ms |
| ⑨ 清理临时目录 | 00:04:49.198 → .200 | 2ms |
| ⑩-A LLM 第 1 次尝试 | 00:04:49.234 → 00:05:36.952 | **47.72s** |
| ⑩-B INSERT video_metadata | 00:05:36.953 → .978 | 25ms |
| ⑩-C UPDATE video.status | 00:05:36.978 → 00:05:37.001 | 23ms |
| **总计 `complete` 接口** | 00:04:46.768 → 00:05:37.001 | **~50.2s** |

**LLM 单步占整个 `complete` 耗时的 95%**。

### 4.3 LLM 失败的放大效应（重试机制）

`QwenLlmServiceAdapter.complete` 有"sleep + 重试"机制（**不是**真正的指数退避算法库，是手写 `Thread.sleep`）：

```properties
# 默认配置（来自 QwenLlmServiceAdapter @Value 默认值）
grace.llm.retry.max-attempts = 3
grace.llm.retry.initial-interval-ms = 1000
grace.llm.retry.backoff-multiplier = 2
```

实际行为（见 `QwenLlmServiceAdapter.complete` 第 64-103 行）：
- attempt 1：调 LLM，失败则 `Thread.sleep(1000ms)`，`backoffInterval *= 2` → 2000ms；
- attempt 2：调 LLM，失败则 `Thread.sleep(2000ms)`，`backoffInterval *= 2` → 4000ms；
- attempt 3：调 LLM，失败则**不再 sleep**，直接抛 `ExternalServiceException(LLM_SERVICE_UNAVAILABLE, 9001)`。

**单次 attempt 的网络层超时取决于 `RestTemplate` 的超时配置**。**已查证 `WebConfig.restTemplate()` 直接 `return new RestTemplate()`，没有任何超时设置**，因此走 JDK `HttpURLConnection` 的默认值，即 `connectTimeout = 0` / `readTimeout = 0`，**两个 0 都表示"无限等待"**。

由此推导出几个最坏场景：
- **LLM 服务端响应正常但很慢**：单次 attempt 可能 hang 几分钟而不被 RestTemplate 主动中断，整个 `complete` 接口跟着 hang；
- **网络断开 / DNS 失败**：依赖 OS TCP 栈的默认超时（Linux 通常 `tcp_syn_retries=6`，~127s 才返回失败）；
- **3 次 attempt 全部网络超时（按 OS 默认 ~127s 估算）**：阻塞 ≈ 127 + 1 + 127 + 2 + 127 ≈ **384s ≈ 6.4 分钟**，事务持有期同样 ≈ 6.4 分钟。

实测 trace 里只跑了 `attempt 1 of 3` 就成功了（耗时 47.7s），所以"只"花了 47.7s；但**当前代码里没有任何机制能保证 LLM 调用在合理时间内失败**，这是异步化改造前必须正视的事实。

### 4.4 为什么说"LLM 是强依赖"

1. **代码路径强耦合**：`completeUpload` 末尾的 `eventPublisher.publish(VideoUploadedEvent)` 是同步调用，监听器内部直接调 `MetadataGenerationServiceImpl.generate` → 直接调 LLM。**没有任何异步队列、消息中间件、线程池隔离**。
2. **事务强耦合**：监听器跑在 `completeUpload` 的事务内，LLM 抛异常 → 事务回滚 → `video` 表 INSERT 也跟着回滚 → **用户感知"上传失败"**，但 OSS 文件已经传上去了，产生孤儿对象。
3. **状态机强耦合**：`video.status` 从 `UPLOADED` 推进到 `METADATA_GENERATED` 必须依赖 LLM 成功。`UPLOADED` 这个中间状态在当前同步链路下**对客户端不可见**（事务还没提交，前端就看不到）。
4. **响应数据强耦合**：`complete` 接口的 `ServerUploadCompleteResponse.status` 字段直接返回 `video.status`，所以前端拿到的状态字面意义就是"LLM 已经处理完了"。

> 异步化改造的核心矛盾在于：**怎么把 LLM 这步从 HTTP 请求路径上拆出去**，同时还要让前端能感知到"AI 元数据已生成"这个事件。但具体方案怎么选，本文档不展开。


## 5. 前端实现细节

### 5.1 关键文件

| 文件 | 职责 |
|---|---|
| `grace-frontend/src/api/client.ts` | axios 实例 + EagleEye traceId 注入 + 响应拦截器（**`timeout: 30000`**） |
| `grace-frontend/src/api/video.ts` | 三个上传接口的瘦封装：`serverUploadInit` / `serverUploadChunk` / `serverUploadComplete` |
| `grace-frontend/src/hooks/useServerUpload.ts` | 上传业务编排 hook：分片切分、循环上传、单分片重试、`complete` 调用、**轮询 video 状态** |
| `grace-frontend/src/pages/VideoUploadPage.tsx` | 页面壳子：拖拽 / 选文件 / 进度卡片 / 已完成列表 / 错误 toast |

### 5.2 axios 实例（`client.ts`）

```ts
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 30000,                              // ← 全局 30s，覆盖所有接口
  headers: { 'Content-Type': 'application/json' },
})
```

请求拦截器：每次请求生成 30 位 EagleEye traceId，写入 `X-EagleEye-TraceId` / `X-EagleEye-RpcId` header，并在 `cfg.metadata` 记录 `start` 时刻。

响应拦截器（关键，是"网络连接失败"假阳性的来源）：

```ts
(error) => {
  // ...console.error 打 trace
  if (error.response) {
    const data = error.response.data
    return Promise.reject(new ApiError(data?.code ?? 9999, data?.message ?? '网络错误', ...))
  }
  return Promise.reject(new ApiError(9999, '网络连接失败'))   // ← axios timeout / abort 走这里
}
```

axios 在 30s 超时时，回调里 `error.response` 是 `undefined`（因为根本没收到响应），所以**走兜底分支抛 `'网络连接失败'`**。后端日志里同 traceId 的请求其实还在跑 LLM。

### 5.3 上传编排（`useServerUpload.ts`）

主流程在 `uploadFile(file)` 内：

```ts
1. setUploadState({ status: 'initializing', ... })
2. const session = await serverUploadInit({ fileName, fileSize, format })
3. setUploadState({ status: 'uploading', uploadId, totalChunks })
4. for (let i = 0; i < session.totalChunks; i++) {
     const chunk = file.slice(i*chunkSize, (i+1)*chunkSize)
     // 单分片最多 3 次 attempt，失败 sleep 1000*attempts ms（线性退避）
     for (let attempts = 0; attempts < MAX_RETRY_ATTEMPTS; attempts++) {
       try { await serverUploadChunk(uploadId, i, chunk); break }
       catch (e) { if (attempts >= 3) throw; await sleep(1000*attempts) }
     }
     // 更新进度、speed、estimatedTime
   }
5. setUploadState({ status: 'completing', progress: 100 })
6. const completeResult = await serverUploadComplete(uploadId)   // ← 同步等 ~50s，会被 30s axios timeout 截断
7. queryClient.invalidateQueries({ queryKey: ['videos'] })
8. const videoStatus = await pollVideoStatus(completeResult.videoId)   // ← 即使 6 拿到了响应，还要轮询
9. setUploadState({ status: 'completed', videoId, videoStatus })
```

`pollVideoStatus`：

```ts
const POLL_INTERVAL = 2000          // 2s 一次
const MAX_POLL_ATTEMPTS = 60        // 最多 60 次
// 调 GET /api/videos/{videoId}，直到 video.status !== 'UPLOADED' 或达到上限
```

> **隐含事实**：当前 `complete` 是同步链路，`completeResult.status` 必然已经是 `METADATA_GENERATED`，**所以 `pollVideoStatus` 第 1 次轮询就能命中，立刻返回**。这块**轮询代码看起来像是为"未来异步化"预留的**，目前没有发挥作用 —— 一旦后端改成异步、`complete` 在 LLM 完成前就返回 `UPLOADED`，这个轮询就会真正生效。

### 5.4 页面层（`VideoUploadPage.tsx`）

- `useServerUpload()` → `uploadFile(file)` 是入口；
- 进度条显示 `uploadState.progress`，文案根据 `status` 切换（initializing / uploading / **completing** / completed / error）；
- 错误兜底：`catch (error) { errorMessage = error?.message ?? '上传失败' }` → toast + `setUploadState({ status: 'error', error })`；
- 因此 axios 抛出的 `ApiError(9999, '网络连接失败')` 会**两处可见**：① 进度卡片底部红字（`UploadProgressCard.statusText`），② 全局 toast。截图里的"网络连接失败"红字就是 ① 。

### 5.5 Vite Dev 代理

```ts
// vite.config.ts
server: {
  port: 3000,
  proxy: {
    '/api': { target: 'http://localhost:8000', changeOrigin: true },
  },
}
```

前端访问 `localhost:3000`，被 Vite Dev Server 代理到后端 `localhost:8000`。axios timeout 在浏览器端，不受代理影响；代理本身**没有显式超时配置**，走 `http-proxy` 默认（无超时）。所以"网络连接失败"的瓶颈点就是 axios 的 `timeout: 30000`，不是 proxy。


## 6. 数据库状态机

### 6.1 涉及的表

`complete` 流程涉及 3 张表的写入（**同一个事务**）：

| 表 | 操作 | 关键字段 |
|---|---|---|
| `upload_session` | UPDATE | `status: ACTIVE → COMPLETED`, `uploaded_chunks` |
| `video` | INSERT → UPDATE | `id`, `file_name`, `file_size`, `format`, `duration_seconds`, `storage_url`, `storage_provider=OSS`, `status: UPLOADED → METADATA_GENERATED`, `ext_info` (JSON) |
| `video_metadata` | INSERT | `id`, `video_id`, `title`, `description`, `tags_json`, `source=AI_GENERATED`, `confirmed=false` |

`video.ext_info` 是 JSON 字段，约定 key：`traceId` / `rpcId` / `createdTraceId`（创建时的 trace，便于回溯，详见 `docs/lessons.md` L1）。

### 6.2 `UploadSessionStatus`（`video/domain/UploadSessionStatus.java`）

```
ACTIVE      ── 会话活跃，可以上传分片
COMPLETED   ── 上传完成，已合并为完整视频
EXPIRED     ── 会话已过期，无法继续上传
```

转移：`ACTIVE → COMPLETED`（在 `completeUpload` 里），`ACTIVE → EXPIRED`（按 `expires_at` 判断，代码里只读不主动扫描）。

### 6.3 `VideoStatus`（`video/domain/VideoStatus.java`）

完整枚举与转移规则**由 `Video.STATUS_TRANSITIONS` 强制约束**（`Video.java:34-50`）：

```
UPLOADED            → { METADATA_GENERATED }
METADATA_GENERATED  → { READY_TO_PUBLISH }
READY_TO_PUBLISH    → { PUBLISHING }
PUBLISHING          → { PUBLISHED, PUBLISH_FAILED }
PUBLISHED           → { PROMOTION_DONE }
PUBLISH_FAILED      → { PUBLISHING }                ← 重试
PROMOTION_DONE      → { }                           ← 终态
DELETED             ← 通过 markAsDeleted() 软删除（PUBLISHED 不可删）
```

调用 `Video.transitionTo(target)` 时，若 `target` 不在当前状态的允许集合里，会抛 `BusinessRuleViolationException(INTERNAL_SERVER_ERROR, "Invalid status transition from X to Y")`。

### 6.4 `complete` 流程内的状态推进顺序（重要）

由于 `Video.createWithStorageUrl` 一上来就把 `status` 置成 `UPLOADED`，且状态机要求 `UPLOADED → METADATA_GENERATED` **必须先有 UPLOADED 落库**，因此当前同步链路里两次 DB 写**不可省略**：

1. `ServerUploadApplicationService.completeUpload` ：`videoRepository.save(video)` → INSERT video (UPLOADED)
2. `MetadataApplicationService.generateMetadata` ：`video.transitionTo(METADATA_GENERATED)` + `videoRepository.save(video)` → UPDATE video.status

异步化改造时，**这两步如果跨事务/跨服务，需要特别处理**：在状态推进的间隙，外界看到的 `video.status` 会是 `UPLOADED`（这正是前端 `pollVideoStatus` 等待的中间态）。


## 7. trace 实证数据（c8cb15ae...）

数据来源：`logs/grace-platform/application.log`。trace `c8cb15ae1777997086754014de5d6c` 完整时间线：

| # | 时刻 | 线程 | 类 | 事件 |
|---|---|---|---|---|
| 1 | 00:04:46.768 | exec-5 | `U.findById` | SELECT upload_session WHERE upload_id = `upl_b766f359f49c4aaf` |
| 2 | 00:04:46.779 | exec-5 | `U.findById` | <== Total: 1 |
| 3 | 00:04:46.780 | exec-5 | `ChunkMergeServiceImpl` | All 1 chunks validated |
| 4 | 00:04:46.780 | exec-5 | `ChunkMergeServiceImpl` | Starting chunk merge: 1 chunks |
| 5 | 00:04:46.781 | exec-5 | `ChunkMergeServiceImpl` | Chunk chunk_0 appended at offset 0 |
| 6 | 00:04:46.788 | exec-5 | `ChunkMergeServiceImpl` | Chunk merge completed (size: 1321275 bytes) |
| 7 | ~00:04:46.9xx | exec-5 | `VideoFileInspectorImpl` | Video file inspected (MP4, duration: 16s) |
| 8 | 00:04:46.956 | exec-5 | `AliOssStorageServiceImpl` | Uploading file to OSS: `videos/20260506/upl_b766f359f49c4aaf_744599de.mp4` |
| 9 | 00:04:49.127 | exec-5 | `AliOssStorageServiceImpl` | File uploaded successfully (rpcId 0.1) |
| 10 | 00:04:49.129 | exec-5 | `VideoMapper.insert` | INSERT video (status=`UPLOADED`, ext_info 含 traceId) |
| 11 | 00:04:49.157 | exec-5 | `VideoMapper.insert` | <== Updates: 1 |
| 12 | 00:04:49.172 | exec-5 | `U.update` | UPDATE upload_session SET status=`COMPLETED` |
| 13 | 00:04:49.197 | exec-5 | `U.update` | <== Updates: 1 |
| 14 | 00:04:49.198 | exec-5 | `ServerUploadApplicationService` | Deleted merged file |
| 15 | 00:04:49.200 | exec-5 | `ServerUploadApplicationService` | Cleaned up temp directory |
| 16 | 00:04:49.202 | exec-5 | `VideoMapper.findById` | （监听器内）SELECT video WHERE id = `vid_a7bba526...` |
| 17 | 00:04:49.222 | exec-5 | `V.findByVideoIdOrdered` | SELECT video_metadata WHERE video_id = ... ORDER BY created_at DESC |
| 18 | 00:04:49.234 | exec-5 | `V.findByVideoIdOrdered` | <== Total: 0（无历史风格） |
| 19 | 00:04:49.234 | exec-5 | `MetadataGenerationServiceImpl` | Generating metadata (text-only mode) |
| 20 | 00:04:49.234 | exec-5 | `QwenLlmServiceAdapter` | LLM request attempt 1 of 3 (mode: text), **rpcId 0.2** |
| 21 | **00:05:36.952** | exec-5 | `MetadataGenerationServiceImpl` | LLM response received, promptTokens=300, completionTokens=2611 |
| 22 | 00:05:36.952 | exec-5 | `MetadataGenerationServiceImpl` | Successfully parsed metadata, tagsCount=12 |
| 23 | 00:05:36.953 | exec-5 | `V.insert` | INSERT video_metadata (`meta_ec05a37e...`) |
| 24 | 00:05:36.978 | exec-5 | `V.insert` | <== Updates: 1 |
| 25 | 00:05:36.978 | exec-5 | `VideoMapper.update` | UPDATE video SET status=`METADATA_GENERATED` |
| 26 | 00:05:37.001 | exec-5 | `VideoMapper.update` | <== Updates: 1 |
| 27 | 00:05:37.001 | exec-5 | `ServerUploadApplicationService` | Server upload completed for video: `vid_a7bba526...` |

**关键观察**：
- 全部 27 条日志都在**同一个线程** `http-nio-8000-exec-5` 上 → 印证"监听器同步执行 + 共用调用栈"。
- traceId 全程一致 `c8cb15ae1777997086754014de5d6c`，rpcId 在 OSS putObject 时变成 `0.1`，在 LLM 调用时变成 `0.2` → 来自 `TraceContext.pushSpan()`（见 `AliOssStorageServiceImpl` line 81 / `MetadataGenerationServiceImpl` line 122）。
- LLM（步骤 20→21）耗时 **47.718s**，占整条 `complete` 接口耗时（46.768→37.001 共 50.233s）的 **95.0%**。
- 整段日志**没有任何 ERROR / WARN / Exception**，从后端视角看是**完全成功**；前端"网络连接失败"完全是 axios 30s 超时导致的客户端假阳性。

> 想自己复现这条 trace 的查询：
> `grep "c8cb15ae1777997086754014de5d6c" logs/grace-platform/application.log`


## 8. 当前架构的已知问题清单（异步化改造前必读）

> 这一节只罗列**当前现状中已经存在的问题**，不展开方案设计（用户已说"准备改成异步链路"，方案由用户拍）。每条都对应具体代码或日志证据，可直接验证。

### P1（阻塞用户体验，已实测发生）

1. **前端 `axios.timeout = 30000` 远小于后端 `complete` 实测耗时（~50s）**
   - 证据：`grace-frontend/src/api/client.ts:53` + trace `c8cb15ae...` 实测 50.2s
   - 影响：UI 显示"网络连接失败"，但后端事务实际成功，**用户感知与系统状态不一致**
   - 截图实证：见用户在第 2 轮对话中提供的 DevTools Network 截图（`complete` 红 ❌、Response headers 空、`X-Eagleeye-Traceid: c8cb15ae...`）

2. **`RestTemplate` 没配任何超时（`new RestTemplate()`）**
   - 证据：`grace-platform/src/main/java/com/grace/platform/shared/infrastructure/config/WebConfig.java:14`
   - 影响：LLM 单次调用最坏可能 hang 数分钟才被 OS TCP 层拒绝，3 次 attempt 累计可达 5+ 分钟，期间 DB 事务和 HTTP 工作线程都被占着

### P2（架构耦合，会限制后续演进）

3. **LLM 调用与 HTTP 请求路径强同步耦合**
   - 调用栈：`completeUpload` → `eventPublisher.publish` →（Spring 同步派发）→ `VideoUploadedEventListener.handle` → `MetadataApplicationService.generateMetadata` → LLM
   - 证据：`SpringDomainEventPublisher` + `@EventListener`（不是 `@TransactionalEventListener`）

4. **LLM 调用与外层 DB 事务强耦合**
   - 监听器和外层共用同一事务（`MetadataApplicationService` `@Transactional` propagation=REQUIRED）→ **事务持有时长 = LLM 耗时 ≈ 50s**
   - 影响：长事务占数据库连接，并发上来后会快速耗尽连接池

5. **LLM 失败 → 整个 `complete` 事务回滚 → OSS 文件孤儿**
   - 证据：`AliOssStorageServiceImpl.uploadFile` 不在事务内（也无法在事务内，OSS 没有事务）；`completeUpload` 内只有 `cleanupTempDirectory` 清本地 temp，**没有任何 OSS 删除 / 标记的补偿逻辑**
   - 影响：每次 LLM 失败都会在 OSS bucket 留一个无引用的视频文件

### P3（功能/语义错位，目前不痛但要知道）

6. **前端 `pollVideoStatus` 是"为异步预留"的死代码**
   - 证据：`grace-frontend/src/hooks/useServerUpload.ts:60-77, 167-174`
   - 当前同步链路下，`completeResult.status` 一回来就已经是 `METADATA_GENERATED`，第 1 次轮询立刻命中返回。**这块代码在异步化后才会真正生效**

7. **`Video` 状态机要求 `UPLOADED → METADATA_GENERATED` 必须先有 UPLOADED 落库**
   - 证据：`Video.STATUS_TRANSITIONS`（`Video.java:36`）+ `Video.transitionTo` 严格校验
   - 影响：异步化时如果想让 video 入库即 `METADATA_GENERATED`（跳过 UPLOADED 中间态）会被状态机拦截，要么修状态机要么走"先 INSERT UPLOADED → 后 UPDATE" 的两步式

8. **`complete` 接口的响应 DTO `ServerUploadCompleteResponse.status` 直接暴露 `video.status`**
   - 异步化后这个字段拿到的会是 `UPLOADED`（不再是 `METADATA_GENERATED`），所有依赖这个字段的前端逻辑都需要相应调整（包括 `useServerUpload.ts:174` 的 `videoStatus` 设定 + `VideoUploadPage.tsx:418` 的 `CompletedUpload.status` 显示）

### P4（运维/可观测性）

9. **没有 metrics / tracing 系统化采集 LLM 调用耗时**
   - 现状：仅靠 `DigestLogger.llm(...)` 写文本日志（`logs/grace-platform/oss-digest.log` 同款）
   - 影响：异步化后想验证"LLM p95/p99 耗时"、"重试率"需要先做埋点

10. **EagleEye traceId 在前端是浏览器随机生成的，跨服务关联只到后端**
    - 证据：`grace-frontend/src/api/client.ts:42-49` 用随机 hex 作为 IP 段
    - 影响：trace 不能关联到 LLM 服务端（Qwen 也没接入同套 trace），LLM 那一段 47s 是黑盒

---

## 附：相关文件清单（一站式索引）

后端：
- `grace-platform/src/main/java/com/grace/platform/video/interfaces/rest/ServerUploadController.java`
- `grace-platform/src/main/java/com/grace/platform/video/application/ServerUploadApplicationService.java`
- `grace-platform/src/main/java/com/grace/platform/video/domain/Video.java`（聚合根 + 状态机）
- `grace-platform/src/main/java/com/grace/platform/video/domain/VideoStatus.java`
- `grace-platform/src/main/java/com/grace/platform/video/domain/UploadSessionStatus.java`
- `grace-platform/src/main/java/com/grace/platform/video/domain/event/VideoUploadedEvent.java`
- `grace-platform/src/main/java/com/grace/platform/video/infrastructure/file/ChunkMergeServiceImpl.java`
- `grace-platform/src/main/java/com/grace/platform/video/infrastructure/file/VideoFileInspectorImpl.java`
- `grace-platform/src/main/java/com/grace/platform/storage/infrastructure/ali/AliOssStorageServiceImpl.java`
- `grace-platform/src/main/java/com/grace/platform/metadata/application/listener/VideoUploadedEventListener.java`
- `grace-platform/src/main/java/com/grace/platform/metadata/application/MetadataApplicationService.java`
- `grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/llm/MetadataGenerationServiceImpl.java`
- `grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/llm/QwenLlmServiceAdapter.java`
- `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/event/SpringDomainEventPublisher.java`
- `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/config/WebConfig.java`（`RestTemplate` 配置点）

前端：
- `grace-frontend/src/api/client.ts`（axios 实例 + 30s timeout + traceId 注入）
- `grace-frontend/src/api/video.ts`（三个上传接口）
- `grace-frontend/src/hooks/useServerUpload.ts`（编排 + 轮询）
- `grace-frontend/src/pages/VideoUploadPage.tsx`（页面壳）
- `grace-frontend/vite.config.ts`（dev server proxy）

实证日志：
- `logs/grace-platform/application.log`（trace `c8cb15ae1777997086754014de5d6c`）

## v2 异步链路（2026-05）

> 设计 spec 详见：[docs/superpowers/specs/2026-05-06-async-video-upload-design.md](superpowers/specs/2026-05-06-async-video-upload-design.md)

### 核心变更

- `complete` 接口从同步 ~50s 改为异步 <500ms 返回（HTTP 202）
- 5 个大步骤（合并/探测/OSS/LLM/清理）由 `upload_task` 表驱动
- Push（@Async 事件）为主路径，Pull（@Scheduled 30s）为兜底
- LLM 终态失败 → `video.status=METADATA_FAILED` + 手动重试接口

### 任务状态机

```
PENDING_MERGE → RUNNING_MERGE → PENDING_INSPECT → ... → DONE
```

每步失败自动重试（最多 5 次），耗尽后 status=FAILED。
METADATA 步骤终态失败会同步推 video.status=METADATA_FAILED，前端可调 `POST /videos/{id}/retry-metadata` 重试。

### 前端适配

- polling 终止条件扩展：`METADATA_GENERATED` 或 `METADATA_FAILED`
- 失败态 UI：红色提示卡片 + "重试元数据生成"按钮
