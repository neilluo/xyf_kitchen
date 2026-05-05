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
前端                                        后端 (8000)                       Qwen LLM
 │                                              │                                │
 ├── POST /init ─────────────────────────────►  │                                │
 │                                              │ 写 upload_session (ACTIVE)     │
 │ ◄── 200 { uploadId, totalChunks, ...}        │                                │
 │                                              │                                │
 ├── POST /{uploadId}/chunk (idx=0) ─────────►  │ Files.copy → ./data/temp/...   │
 │ ◄── 200                                      │                                │
 ├── POST /{uploadId}/chunk (idx=1) ─────────►  │                                │
 │ ◄── 200                                      │                                │
 │   ...（共 totalChunks 次）                    │                                │
 │                                              │                                │
 ├── POST /{uploadId}/complete ──────────────►  │ ① ChunkMergeService            │
 │                                              │ ② VideoFileInspector (ffprobe) │
 │           ⏳ 同步等待 ~50s                    │ ③ AliOss putObject              │
 │           ⚠ axios timeout=30s                 │ ④ INSERT video (UPLOADED)       │
 │           → 触发"网络连接失败"                  │ ⑤ generate metadata ─────────► LLM (47s)
 │                                              │                          ◄─────│
 │                                              │ ⑥ INSERT video_metadata         │
 │                                              │ ⑦ UPDATE video.status            │
 │                                              │     = METADATA_GENERATED         │
 │                                              │ ⑧ publish VideoUploadedEvent    │
 │ ◄── 200 (实际后端返回了，但前端已 abort)       │                                │
```

---

## 2. 接口定义

（待填充）

## 3. 后端实现细节

（待填充）

## 4. LLM 调用：当前链路的强依赖

（待填充）

## 5. 前端实现细节

（待填充）

## 6. 数据库状态机

（待填充）

## 7. trace 实证数据（c8cb15ae...）

（待填充）

## 8. 当前架构的已知问题清单（异步化改造前必读）

（待填充）
