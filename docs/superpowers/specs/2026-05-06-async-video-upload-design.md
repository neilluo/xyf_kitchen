# 视频上传链路异步化与可恢复化设计

> 日期：2026-05-06
> 关联基线文档：[docs/video_upload.md](../../video_upload.md)（当前同步链路实测基线）
> 关联记忆：`视频上传全链路异步化与可恢复性要求`、`视频上传单链路架构`

## 1. 背景与目标

### 1.1 现状痛点（来自 `docs/video_upload.md` 第 8 节）
- `complete` 接口同步耗时 ~50s，其中 **LLM 占 95%**（实测 47.7s）；前端 `axios.timeout=30s` 远小于后端耗时，触发"网络连接失败"假阳性。
- LLM 调用通过 Spring 同步事件派发，**复用外层事务**，事务持有 ~50s，长事务占数据库连接。
- LLM 失败 → 整个事务回滚 → `video` 行消失，但 OSS 文件已落地 → **OSS 孤儿**。
- 重启即丢：任何一步失败后，当前架构**没有任何机制**能自动恢复并推进到终态。

### 1.2 本次目标
1. **全链路异步化**：`complete` 接口改为"立即返回 + 后台任务驱动"。
2. **大步骤可恢复**：合并 / 探测 / OSS / LLM / 清理这 5 个大步骤任一失败都能通过 task 表自动重试或手动介入，最终一致。
3. **应用重启安全**：重启后能从 DB 状态继续推进未完成任务。
4. **手动重试入口**：LLM 终态失败后，前端可触发重试接口。

### 1.3 非目标（本次明确不做）
- 前端分片断点续传（保留当前"前端 → 后端磁盘 → OSS"单链路）。
- 前端直传 OSS / STS 签名方案。
- DAG 式任务调度引擎（采用粗粒度状态机，不引入 DolphinScheduler 等）。
- OSS 孤儿文件反向清理（后续独立任务处理）。
- 多实例水平扩展（设计保留可能性，不在本次验证）。

## 2. 整体架构

### 2.1 新数据流

```
前端                              后端 (Server 线程池)                            Worker 线程池                    Qwen LLM
 │                                       │                                            │                               │
 ├── POST /init ─────────────────────►   │ INSERT upload_session (ACTIVE)             │                               │
 │ ◄── 200 { uploadId, ... }             │                                            │                               │
 │                                       │                                            │                               │
 ├── POST /chunk  (循环 N 次) ──────►    │ Files.copy + UPDATE session                │                               │
 │ ◄── 200                               │                                            │                               │
 │                                       │                                            │                               │
 ├── POST /complete ──────────────────►  │ ① 事务：                                   │                               │
 │                                       │    INSERT video (status=UPLOADED)          │                               │
 │                                       │    INSERT upload_task (step=PENDING_MERGE) │                               │
 │                                       │    UPDATE session COMPLETED                │                               │
 │                                       │ ② 提交事务                                  │                               │
 │                                       │ ③ @Async publish TaskReadyEvent ──────────►│                               │
 │ ◄── 202 { videoId, status=UPLOADED }  │ ④ return（毫秒级）                          │                               │
 │                                       │                                            ├─ SELECT ... FOR UPDATE        │
 │  前端 pollVideoStatus (已有代码)       │                                            │  SKIP LOCKED                   │
 │    → GET /videos/{id}                 │                                            │  确认 task 可执行              │
 │                                       │                                            ├─ MERGE   （独立小事务）         │
 │                                       │                                            ├─ INSPECT （独立小事务）         │
 │                                       │                                            ├─ OSS_UPLOAD                    │
 │                                       │                                            ├─ GENERATE_METADATA ────POST───►│
 │                                       │                                            │                         ◄─────│
 │                                       │                                            ├─ CLEANUP                       │
 │                                       │                                            └─ UPDATE task (DONE)             │
 │                                       │                                                                             │
 │                                       │ 兜底：@Scheduled(30s) 扫描                  │                               │
 │                                       │   - PENDING > 60s 未被 push 消费 → 主动拉起 │                               │
 │                                       │   - RUNNING 但 heartbeat > 2min → 重置      │                               │
```

### 2.2 核心组件一览

| 新增 / 修改 | 类型 | 职责 |
|---|---|---|
| `upload_task` | 新表 | 异步任务状态机（一视频一行，粗粒度） |
| `VideoStatus.METADATA_FAILED` | 新枚举值 | LLM 终态失败的可恢复态 |
| `UploadTaskRepository` / `UploadTaskMapper` | 新 | MyBatis CRUD + 原子 claim |
| `VideoUploadPipelineExecutor` | 新 | 5 个大步骤的 switch-case 驱动器 |
| `UploadTaskPushListener` | 新 | `@Async` 监听 `TaskReadyEvent`，调用 executor |
| `UploadTaskPullScheduler` | 新 | `@Scheduled(30s)` 扫 PENDING / 僵尸任务 |
| `ServerUploadApplicationService` | 改 | `completeUpload` 事务内 INSERT task，不再同步做后续 |
| `VideoUploadedEventListener` | **删** | 事件链改走 task，不再由 domain event 触发 LLM |
| `MetadataApplicationService.generateMetadata` | 改 | 由 executor 直接调用，去掉 `@Transactional` 类级注解，改方法级独立事务 |
| `POST /videos/{id}/retry-metadata` | 新接口 | 手动重试入口，把 task 从 `FAILED` 推回 `PENDING_METADATA` |
| `Video.STATUS_TRANSITIONS` | 改 | 新增 `UPLOADED → METADATA_FAILED`、`METADATA_FAILED → METADATA_GENERATED` |

## 3. 数据库设计

### 3.1 新增表 `upload_task`

```sql
CREATE TABLE upload_task (
    id                VARCHAR(36)  NOT NULL PRIMARY KEY,           -- task_xxxxxxxxxxxxxxxx
    video_id          VARCHAR(36)  NOT NULL,                        -- 关联 video.id（1:1）
    current_step      VARCHAR(30)  NOT NULL DEFAULT 'PENDING_MERGE',-- 见 3.2 枚举
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',      -- PENDING / RUNNING / FAILED / DONE
    attempts          INT          NOT NULL DEFAULT 0,              -- 当前 step 已尝试次数
    max_attempts      INT          NOT NULL DEFAULT 5,              -- 每 step 最多尝试次数
    last_error        TEXT,                                         -- 最近一次失败堆栈摘要
    owner_instance_id VARCHAR(64),                                  -- 抢到锁的实例 ID（多实例预留）
    heartbeat_at      TIMESTAMP    NULL,                            -- 运行中每步定期更新；NULL = 未运行
    payload           TEXT,                                         -- JSON：{ uploadId, fileName, tempDir, mergedFilePath, ossUrl, ... }
    ext_info          TEXT,                                         -- JSON：{ traceId, createdTraceId, ... }
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_upload_task_video_id (video_id),
    INDEX idx_upload_task_status_step (status, current_step),
    INDEX idx_upload_task_heartbeat (status, heartbeat_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**设计说明**：
- `video_id` UNIQUE：一视频一行（粗粒度决策）。
- `(status, current_step)` 复合索引：服务 pull scheduler 的 `WHERE status='PENDING' AND ...` 和 dashboard 聚合查询。
- `(status, heartbeat_at)` 复合索引：服务"僵尸任务扫描"（RUNNING 且 heartbeat 过期）。
- `payload` JSON：各 step 间传递的上下文，避免每步都查 `video` / `upload_session` 表。
- `ext_info` 遵循现有约定（同 `video.ext_info`），写入 `traceId` / `createdTraceId` 便于全链路关联。

**`docs/lessons.md` L1 规则对齐**：改 DDL 必须同步 `docs/backend/db-design.md`，本次落地时一并补。

### 3.2 `upload_task.current_step` 枚举（顺序推进）

```
PENDING_MERGE   ─ step 尚未开始；初始值
RUNNING_MERGE   ─ worker 正在合并分片
PENDING_INSPECT ─ 合并完成，等待探测
RUNNING_INSPECT
PENDING_OSS
RUNNING_OSS
PENDING_METADATA
RUNNING_METADATA
PENDING_CLEANUP
RUNNING_CLEANUP
DONE            ─ 终态
```

转移规则（纯顺序，无分支）：

```
PENDING_X ──(worker claim)──► RUNNING_X ──(success)──► PENDING_{next}
                              RUNNING_X ──(fail, attempts<max)──► PENDING_X (retry)
                              RUNNING_X ──(fail, attempts>=max)──► FAILED (留在当前 step)
```

### 3.3 `upload_task.status` 与 `current_step` 的关系

| status | current_step 含义 |
|---|---|
| `PENDING` | 下一次被 claim 时会执行 `current_step` 所指的动作 |
| `RUNNING` | 正在执行 `current_step`，`heartbeat_at` 需周期性更新 |
| `FAILED` | `current_step` 上耗尽 `max_attempts`，等待手动重试或放弃 |
| `DONE` | `current_step=DONE`，整条流水线完成 |

### 3.4 `video` 状态机扩展

新增枚举值 `VideoStatus.METADATA_FAILED`（原枚举见 [VideoStatus.java](../../../grace-platform/src/main/java/com/grace/platform/video/domain/VideoStatus.java)）。

`Video.STATUS_TRANSITIONS`（[Video.java:34-50](../../../grace-platform/src/main/java/com/grace/platform/video/domain/Video.java#L34-L50)）变更：

```java
// 变更前
transitions.put(UPLOADED, Set.of(METADATA_GENERATED));

// 变更后
transitions.put(UPLOADED,         Set.of(METADATA_GENERATED, METADATA_FAILED));
transitions.put(METADATA_FAILED,  Set.of(METADATA_GENERATED));  // 手动/自动重试成功后回到正常态
```

**语义对照表**：

| video.status | 含义 | 如何达到 |
|---|---|---|
| `UPLOADED` | 视频字节已入库（可能还在合并/OSS 阶段），元数据未生成 | `complete` 接口返回时即此状态 |
| `METADATA_GENERATED` | LLM 成功、元数据已入库 | task `GENERATE_METADATA` step 成功 |
| `METADATA_FAILED` | LLM 重试耗尽仍失败 | task `RUNNING_METADATA` step `attempts>=max` |

**注意**：`MERGE / INSPECT / OSS_UPLOAD / CLEANUP` 的失败**不反映到 `video.status`**，只体现在 `upload_task.current_step + status=FAILED`。这是刻意简化——前端只关心"元数据好了没"，内部流水线细节不外溢。

## 4. Worker 实现

### 4.1 Push 路径（正常路径，~0ms 延迟）

```java
// ServerUploadApplicationService.completeUpload（伪码）
@Transactional
public ServerUploadCompleteDTO completeUpload(String uploadId) {
    // ...（合并/探测/OSS 的同步工作全部下沉到 worker，这里只做：）
    Video video = Video.createInitial(uploadId, fileName, fileSize, format);
    videoRepository.save(video);                                  // INSERT status=UPLOADED（空 storageUrl）
    UploadTask task = UploadTask.createInitial(video.getId(), payload);
    uploadTaskRepository.save(task);                              // INSERT current_step=PENDING_MERGE
    session.markAsCompleted();
    uploadSessionRepository.save(session);
    // 事务提交后触发 Push（用 @TransactionalEventListener AFTER_COMMIT）
    eventPublisher.publish(new TaskReadyEvent(task.getId()));
    return toDto(video, task);                                    // videoId + status=UPLOADED
}

// UploadTaskPushListener
@Component
public class UploadTaskPushListener {
    @Async("uploadTaskExecutor")
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void on(TaskReadyEvent event) {
        pipelineExecutor.runOnce(event.getTaskId());
    }
}
```

关键点：
- `@TransactionalEventListener(AFTER_COMMIT)`：确保 worker 不会在事务还没提交时读不到 task 行。
- `@Async("uploadTaskExecutor")`：用专用线程池，不阻塞 HTTP 工作线程。

### 4.2 Pull 路径（兜底，30s 粒度）

```java
@Component
public class UploadTaskPullScheduler {
    @Scheduled(fixedDelay = 30_000)
    public void scan() {
        // 1) 孤儿 PENDING：被 Push 丢掉的（应用重启 / @Async 异常）
        List<String> orphanIds = taskRepository.findOrphanPending(60);   // created_at > 60s 前 且 updated_at 也超过 60s
        orphanIds.forEach(pipelineExecutor::runOnce);

        // 2) 僵尸 RUNNING：worker 挂了
        int reset = taskRepository.resetZombie(120);                     // heartbeat_at > 120s 前
        if (reset > 0) log.warn("reset {} zombie tasks", reset);
    }
}
```

### 4.3 `VideoUploadPipelineExecutor.runOnce`（核心驱动器）

```java
public void runOnce(String taskId) {
    UploadTask task = taskRepository.claim(taskId, instanceId);      // UPDATE ... SET status=RUNNING WHERE id=? AND status IN (PENDING) -- 0 行 = 别人抢到了，直接返回
    if (task == null) return;
    try {
        switch (task.getCurrentStep()) {
            case PENDING_MERGE:    doMerge(task); break;
            case PENDING_INSPECT:  doInspect(task); break;
            case PENDING_OSS:      doOssUpload(task); break;
            case PENDING_METADATA: doGenerateMetadata(task); break;
            case PENDING_CLEANUP:  doCleanup(task); break;
            default: /* DONE 或意外 RUNNING_*，不处理 */ return;
        }
        // 每个 doXxx 内部在自己的小事务里：执行动作 + 推进到 PENDING_{next} + status=PENDING
        // 成功后 re-publish 下一步 event，实现"Push 链式推进"
        if (task.getCurrentStep() != DONE) {
            eventPublisher.publish(new TaskReadyEvent(task.getId()));
        }
    } catch (Exception e) {
        taskRepository.markFail(task.getId(), e);                    // attempts++, status=FAILED or back to PENDING
    }
}
```

### 4.4 幂等与原子 claim

- `claim(taskId)` 的 SQL：
  ```sql
  UPDATE upload_task
     SET status='RUNNING', owner_instance_id=?, heartbeat_at=NOW(), attempts=attempts+1
   WHERE id=? AND status='PENDING'
  ```
  `affectedRows` 返回 0 → 说明别人抢到了 / 状态不对 → 当前调用直接 return。
- 每个 `doXxx` 方法必须幂等：
  - `doMerge`：合并前先检查目标合并文件是否已存在且大小等于 `session.fileSize`；是则跳过合并。
  - `doOssUpload`：OSS SDK 的 `putObject` 本身幂等（同 key 覆盖）；但成功后要把 `storageUrl` 写回 `payload`，下一次重入直接读。
  - `doGenerateMetadata`：先 `SELECT video_metadata WHERE video_id=?`；已存在 → 跳过 LLM，直接推进到下一步。
  - `doCleanup`：基于 `Files.exists` 判断再删。

### 4.5 心跳机制

- LLM step（预期 50s+）：在调 `QwenLlmServiceAdapter.complete` **之前**用一个独立守护线程，每 30s `UPDATE heartbeat_at=NOW()`；调用返回（成功/失败）后立即停。
- 其他短 step（<30s）：step 入口处 UPDATE 一次 heartbeat 即可，不起守护线程。
- 僵尸判定阈值：`heartbeat_at < NOW() - 120s` 视为死亡，scheduler 把它重置为 `PENDING`（不 attempts++，因为上次"失败"不是业务失败）。

### 4.6 关键配置（Spring 属性）

```properties
# 专用 worker 线程池
grace.upload-task.executor.core-pool-size = 4
grace.upload-task.executor.max-pool-size = 8
grace.upload-task.executor.queue-capacity = 50

# 兜底 scheduler
grace.upload-task.pull.scan-interval-ms = 30000
grace.upload-task.pull.orphan-threshold-s = 60
grace.upload-task.pull.zombie-threshold-s = 120

# 重试
grace.upload-task.max-attempts = 5
```

## 5. 接口变更

### 5.1 `POST /api/videos/upload/server/{uploadId}/complete`（改）

**变更前**：同步等 ~50s，返回 `status=METADATA_GENERATED`。
**变更后**：毫秒级返回 `status=UPLOADED`，同时附带 `taskId`。

响应体示例：
```json
{
  "videoId": "vid_xxxx",
  "taskId": "task_xxxx",
  "fileName": "...",
  "fileSize": 1321275,
  "format": "MP4",
  "status": "UPLOADED",
  "createdAt": "2026-05-06T..."
}
```

HTTP 状态码：**202 Accepted**（语义上更贴切；前端代码无需改动，2xx 即成功）。

### 5.2 `GET /api/videos/{videoId}`（微调）

响应体在原有基础上新增 `task` 字段，便于前端展示"卡在哪一步"：
```json
{
  "videoId": "...",
  "status": "UPLOADED",
  "task": {
    "currentStep": "RUNNING_METADATA",
    "status": "RUNNING",
    "attempts": 1,
    "lastError": null
  }
}
```

### 5.3 `POST /api/videos/{videoId}/retry-metadata`（新）

触发条件：`video.status = METADATA_FAILED`
作用：把对应 `upload_task` 从 `FAILED` 重置回 `PENDING_METADATA`，`attempts=0`，然后 publish `TaskReadyEvent`。

请求体：空
响应体：
```json
{ "taskId": "...", "currentStep": "PENDING_METADATA", "status": "PENDING" }
```

失败情况：
- `video.status ≠ METADATA_FAILED` → 400 `INVALID_STATE`
- 任务已在 RUNNING → 409 `ALREADY_RUNNING`

**`docs/lessons.md` 对齐**：本接口要同步更新 `api.md`。

## 6. 前端改动

### 6.1 已经预留好的部分（**无需改动**）
- `useServerUpload.ts` 的 `pollVideoStatus`：原本是死代码，异步化后直接生效，2s 一次轮询 `GET /videos/{id}` 直到 `status !== 'UPLOADED'`。
- `axios.timeout=30000`：现在 `complete` 毫秒级返回，30s 绰绰有余，不再触发"网络连接失败"假阳性。

### 6.2 必须新增的部分
- `pollVideoStatus` 的终止条件扩展：增加 `METADATA_FAILED` 作为退出态（否则会轮询到 MAX_POLL_ATTEMPTS=60 才放弃）。
- 失败态 UI：在 `VideoUploadPage` / 视频详情页，当 `status=METADATA_FAILED` 时展示"元数据生成失败，[重试]"按钮，点击调 `POST /videos/{id}/retry-metadata`。
- 进度展示（可选，nice-to-have）：轮询结果里 `task.currentStep` 映射成"合并中 / 上传中 / 生成元数据中..."，比纯"处理中"友好。

### 6.3 API 定义（`grace-frontend/src/api/video.ts`）
- `serverUploadComplete` 响应类型新增 `taskId` 字段。
- 新增 `retryMetadata(videoId)` 函数。
- `getVideo` 响应类型新增可选 `task` 字段。

## 7. 迁移与回滚

### 7.1 数据库迁移
- 新 Flyway 脚本：`V11__create_upload_task.sql`（DDL 见 §3.1）
- 无存量数据需要回填：现存 `video` 都已是 `METADATA_GENERATED` 或更后态，不会触发新路径。

### 7.2 部署顺序
1. 先部署后端（含新表、executor、两个接口），此时前端还没改，`complete` 返回里多的 `taskId` 字段前端会忽略，无影响。
2. 验证一轮：通过 `grace-db-query.sh` 看 task 表跑通。
3. 再部署前端（pollVideoStatus 扩展终止条件、METADATA_FAILED 重试 UI）。

### 7.3 回滚
- 后端回滚：切回旧 jar；新表留着不清，数据无引用。
- DDL 不回滚（upload_task 表存在对旧逻辑无影响）。

## 8. 风险与取舍

| 风险 | 触发条件 | 缓解 |
|---|---|---|
| LLM 长期不可用 → 所有 task 卡在 RUNNING_METADATA | Qwen 全网故障 | `max_attempts=5` 后自动转 METADATA_FAILED，不堆积；前端提示用户稍后重试 |
| non-LLM step（MERGE/INSPECT/OSS/CLEANUP）耗尽 attempts 后前端不可见 | 磁盘损坏 / OSS 长期故障 / ffprobe 崩溃 | 本次刻意不做前端 UI（概率极低，业务含义复杂）。运维侧：`grace-db-query.sh` 查 `upload_task WHERE status='FAILED'`，排查后 UPDATE 为 PENDING 重启流水线。未来若频发再补 UI |
| 单实例重启丢进程 → RUNNING 任务变僵尸 | 应用升级 / Crash | Pull scheduler 120s 后自动 reset；对 LLM 长 step 会损失一次 50s 等待，但语义正确 |
| 多实例部署时 Push 事件只触达一个节点 | 未来水平扩展 | 本次单实例不涉及；未来可把 `@TransactionalEventListener` 改为外部消息总线（Redis Streams / RocketMQ） |
| OSS 孤儿仍然会产生 | `PENDING_OSS` 之后任一步失败且清理前死亡 | 本次不处理，记入后续清理任务 |
| `RestTemplate` 仍无超时 | 现状 | 本次改造顺带加：`connectTimeout=10s`, `readTimeout=120s`（LLM 单次不超过 2min） |
| `upload_task.payload` JSON 膨胀 | 极端大 payload | payload 只存路径/URL/少量字段，不存文件内容；TEXT 足够 |

## 9. 配套文档同步清单（必做，对齐 `docs/lessons.md` L1）

- [ ] `docs/backend/db-design.md`：新增 `upload_task` 表章节 + 索引清单
- [ ] `api.md`：新增 `POST /videos/{id}/retry-metadata`、`GET /videos/{id}` 响应补字段、`complete` 响应改为 202 + 补 taskId
- [ ] `architecture.md`：更新视频上传数据流图
- [ ] `docs/video_upload.md`：新增"v2 异步链路"章节（保留 v1 基线）
- [ ] `docs/lessons.md`：如有新坑点，追加到顶部

## 10. 成功判定（goal-driven，对齐 Karpathy 第 4 条）

改造完成后，以下实测可重复：

1. **前端体感**：`complete` 接口 ≤ 500ms 返回；完整流水线 ≤ 60s 完成（主要由 LLM 决定）。
2. **可恢复实验 A**：在 worker 执行 `RUNNING_OSS` 期间 `kill -9` 后端进程 → 30s 内重启 → 再过 ≤ 150s，video.status 最终为 `METADATA_GENERATED`。
3. **可恢复实验 B**：mock LLM 前 4 次返回 500、第 5 次成功 → task 最终 `DONE`、video `METADATA_GENERATED`、`attempts=5`。
4. **终态失败实验**：mock LLM 5 次全 500 → video 变 `METADATA_FAILED`；调 `POST /videos/{id}/retry-metadata` → mock 改成成功 → 最终 `METADATA_GENERATED`。
5. **日志链路**：单次上传全链路 traceId 贯通 `complete → task push → 各 step`，`grep <traceId> logs/grace-platform/application.log` 可一把拉完整时间线。

---

**下一步**：本 spec 落盘后，转交 `writing-plans` skill 输出可执行的实施计划（DB migration → executor/scheduler → 接口 → 前端的分步骤 task list）。
