# 视频上传链路异步化与可恢复化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把视频上传 `complete` 接口从同步 ~50s 改为异步 <500ms 返回，5 个大步骤（合并/探测/OSS/LLM/清理）由 `upload_task` 表驱动，任一步骤失败可自动重试或手动恢复，最终一致。

**Architecture:** 新增 `upload_task` 表（一视频一行 + `current_step` 状态机），`@Async` 事件触发为主路径（Push），`@Scheduled(30s)` 扫描为兜底（Pull）。LLM 终态失败 → `video.status=METADATA_FAILED` + 手动重试接口。单链路"前端→后端磁盘→OSS"保留不动。

**Tech Stack:** Spring Boot 3 + MyBatis + Flyway (MySQL) + React 19 + Vite + Playwright（E2E）

**Spec reference:** `docs/superpowers/specs/2026-05-06-async-video-upload-design.md`

---

## 文件结构总览

**新建**（后端）
- `grace-platform/src/main/resources/db/migration/V11__create_upload_task.sql`
- `grace-platform/src/main/java/com/grace/platform/video/domain/UploadTask.java`
- `grace-platform/src/main/java/com/grace/platform/video/domain/UploadTaskStatus.java`
- `grace-platform/src/main/java/com/grace/platform/video/domain/UploadTaskStep.java`
- `grace-platform/src/main/java/com/grace/platform/video/domain/UploadTaskRepository.java`
- `grace-platform/src/main/java/com/grace/platform/video/infrastructure/persistence/UploadTaskMapper.java`
- `grace-platform/src/main/java/com/grace/platform/video/infrastructure/persistence/UploadTaskRepositoryImpl.java`
- `grace-platform/src/main/resources/mapper/UploadTaskMapper.xml`
- `grace-platform/src/main/java/com/grace/platform/video/application/VideoUploadPipelineExecutor.java`
- `grace-platform/src/main/java/com/grace/platform/video/application/UploadTaskPushListener.java`
- `grace-platform/src/main/java/com/grace/platform/video/application/UploadTaskPullScheduler.java`
- `grace-platform/src/main/java/com/grace/platform/video/application/event/TaskReadyEvent.java`
- `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/config/AsyncConfig.java`

**修改**（后端）
- `grace-platform/src/main/java/com/grace/platform/video/domain/VideoStatus.java`（+METADATA_FAILED）
- `grace-platform/src/main/java/com/grace/platform/video/domain/Video.java`（STATUS_TRANSITIONS）
- `grace-platform/src/main/java/com/grace/platform/video/application/ServerUploadApplicationService.java`（拆分）
- `grace-platform/src/main/java/com/grace/platform/video/interfaces/rest/ServerUploadController.java`（DTO 补 taskId）
- `grace-platform/src/main/java/com/grace/platform/video/interfaces/rest/VideoUploadController.java`（+GET 返回 task、+retry 接口）
- `grace-platform/src/main/java/com/grace/platform/video/application/VideoApplicationService.java`（+retryMetadata）
- `grace-platform/src/main/java/com/grace/platform/metadata/application/MetadataApplicationService.java`（方法级事务）
- `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/config/WebConfig.java`（RestTemplate 超时）
- **删除**：`grace-platform/src/main/java/com/grace/platform/metadata/application/listener/VideoUploadedEventListener.java`

**新建 / 修改**（前端）
- `grace-frontend/src/types/video.ts`（+task 字段 + METADATA_FAILED）
- `grace-frontend/src/api/video.ts`（+retryMetadata、complete 响应补 taskId）
- `grace-frontend/src/hooks/useServerUpload.ts`（pollVideoStatus 扩展）
- `grace-frontend/src/pages/VideoUploadPage.tsx`（失败态 UI + 重试按钮）

**文档同步**（本次强制）
- `docs/backend/db-design.md`（+upload_task 章节 + 索引清单）
- `api.md`（+retry-metadata、+GET /videos/{id} task 字段、+complete 响应变更）
- `architecture.md`（更新数据流图）
- `docs/video_upload.md`（追加"v2 异步链路"章节）

---

## Task 1: DB Migration — 建 `upload_task` 表

**Files:**
- Create: `grace-platform/src/main/resources/db/migration/V11__create_upload_task.sql`
- Modify: `docs/backend/db-design.md`（新增章节 §2 `upload_task` 表 + §3 索引清单）

- [ ] **Step 1：写 Flyway 迁移脚本**

```sql
-- V11__create_upload_task.sql
CREATE TABLE upload_task (
    id                VARCHAR(36)  NOT NULL PRIMARY KEY,
    video_id          VARCHAR(36)  NOT NULL,
    current_step      VARCHAR(30)  NOT NULL DEFAULT 'PENDING_MERGE',
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts          INT          NOT NULL DEFAULT 0,
    max_attempts      INT          NOT NULL DEFAULT 5,
    last_error        TEXT,
    owner_instance_id VARCHAR(64),
    heartbeat_at      TIMESTAMP    NULL,
    payload           TEXT,
    ext_info          TEXT,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_upload_task_video_id (video_id),
    INDEX idx_upload_task_status_step (status, current_step),
    INDEX idx_upload_task_heartbeat (status, heartbeat_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 2：本地启动应用，验证 Flyway 成功执行**

Run: `cd grace-platform && mvn spring-boot:run`
Expected：日志出现 `Successfully applied 1 migration to schema ... (execution time ...): V11`，MySQL 里 `SHOW TABLES LIKE 'upload_task'` 返回一行。

- [ ] **Step 3：同步 `docs/backend/db-design.md`**

在 §2 全量 DDL 的 Phase 2 块末尾追加 `upload_task` 完整 DDL；在 §3 索引清单追加 3 条索引（`uk_upload_task_video_id` / `idx_upload_task_status_step` / `idx_upload_task_heartbeat`）。

- [ ] **Step 4：提交**

```bash
git add grace-platform/src/main/resources/db/migration/V11__create_upload_task.sql docs/backend/db-design.md
git commit -m "feat(db): 新增 upload_task 表用于异步视频上传任务驱动"
```

---

## Task 2: 扩展 `VideoStatus` 枚举与 `Video` 状态机

**Files:**
- Modify: `grace-platform/src/main/java/com/grace/platform/video/domain/VideoStatus.java`
- Modify: `grace-platform/src/main/java/com/grace/platform/video/domain/Video.java:34-50`（STATUS_TRANSITIONS）
- Test: `grace-platform/src/test/java/com/grace/platform/video/VideoUnitTest.java`（新增测试方法）

- [ ] **Step 1：写失败测试**

在 `VideoUnitTest` 追加：

```java
@Test
void uploaded_can_transit_to_metadata_failed() {
    Video v = newUploadedVideo();
    v.transitionTo(VideoStatus.METADATA_FAILED);
    assertEquals(VideoStatus.METADATA_FAILED, v.getStatus());
}

@Test
void metadata_failed_can_retry_to_metadata_generated() {
    Video v = newUploadedVideo();
    v.transitionTo(VideoStatus.METADATA_FAILED);
    v.transitionTo(VideoStatus.METADATA_GENERATED);
    assertEquals(VideoStatus.METADATA_GENERATED, v.getStatus());
}
```

`newUploadedVideo()` 沿用文件里已有的 helper（无则抄 `VideoUnitTest` 其他测试的构造模式）。

- [ ] **Step 2：运行测试确认失败**

Run: `cd grace-platform && mvn -pl . test -Dtest=VideoUnitTest#uploaded_can_transit_to_metadata_failed`
Expected：编译错误 `METADATA_FAILED` 未定义。

- [ ] **Step 3：加枚举值**

`VideoStatus.java` 在 `UPLOADED` 之后 `METADATA_GENERATED` 之前插入：

```java
METADATA_FAILED,
```

- [ ] **Step 4：改 `Video.STATUS_TRANSITIONS`（L34-50）**

```java
transitions.put(VideoStatus.UPLOADED,        Set.of(VideoStatus.METADATA_GENERATED, VideoStatus.METADATA_FAILED));
transitions.put(VideoStatus.METADATA_FAILED, Set.of(VideoStatus.METADATA_GENERATED));
```

- [ ] **Step 5：运行测试确认通过**

Run: `mvn -pl . test -Dtest=VideoUnitTest`
Expected：全部通过（新 2 个 + 已有）。

- [ ] **Step 6：提交**

```bash
git add grace-platform/src/main/java/com/grace/platform/video/domain/VideoStatus.java \
        grace-platform/src/main/java/com/grace/platform/video/domain/Video.java \
        grace-platform/src/test/java/com/grace/platform/video/VideoUnitTest.java
git commit -m "feat(video): 状态机新增 METADATA_FAILED 态与重试转移"
```

---

## Task 3: `UploadTask` 领域对象 + 枚举

**Files:**
- Create: `UploadTaskStatus.java`, `UploadTaskStep.java`, `UploadTask.java`
- Test: `grace-platform/src/test/java/com/grace/platform/video/UploadTaskUnitTest.java`

- [ ] **Step 1：写枚举 `UploadTaskStatus`**

```java
package com.grace.platform.video.domain;
public enum UploadTaskStatus { PENDING, RUNNING, FAILED, DONE }
```

- [ ] **Step 2：写枚举 `UploadTaskStep`**

```java
package com.grace.platform.video.domain;
public enum UploadTaskStep {
    PENDING_MERGE, RUNNING_MERGE,
    PENDING_INSPECT, RUNNING_INSPECT,
    PENDING_OSS, RUNNING_OSS,
    PENDING_METADATA, RUNNING_METADATA,
    PENDING_CLEANUP, RUNNING_CLEANUP,
    DONE;

    /** 根据当前 PENDING_X 返回 RUNNING_X；反之 RUNNING_X 的下一 PENDING_{next} 用 nextPending() */
    public UploadTaskStep toRunning() { /* switch: PENDING_MERGE -> RUNNING_MERGE, ...*/ }
    public UploadTaskStep nextPending() { /* switch: RUNNING_MERGE -> PENDING_INSPECT, RUNNING_CLEANUP -> DONE ...*/ }
    public boolean isPending() { return name().startsWith("PENDING_"); }
    public boolean isRunning() { return name().startsWith("RUNNING_"); }
}
```

实现细节：两个 switch-case 明确写全 5 对。

- [ ] **Step 3：写 `UploadTask` 聚合根**

关键字段（与 DB 表一一对应）：`id, videoId, currentStep, status, attempts, maxAttempts, lastError, ownerInstanceId, heartbeatAt, payload (Map<String,Object>), extInfo (Map<String,Object>), createdAt, updatedAt`。

静态工厂：

```java
public static UploadTask createInitial(String videoId, Map<String,Object> payload, Map<String,Object> extInfo) {
    UploadTask t = new UploadTask();
    t.id = "task_" + RandomUtil.hex16();
    t.videoId = videoId;
    t.currentStep = UploadTaskStep.PENDING_MERGE;
    t.status = UploadTaskStatus.PENDING;
    t.attempts = 0;
    t.maxAttempts = 5;
    t.payload = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    t.extInfo = extInfo == null ? new LinkedHashMap<>() : new LinkedHashMap<>(extInfo);
    t.createdAt = LocalDateTime.now();
    t.updatedAt = t.createdAt;
    return t;
}
```

领域行为方法（后续 executor 会调）：
- `markRunning(String instanceId)` — `status=RUNNING`, `currentStep=currentStep.toRunning()`, `ownerInstanceId=...`, `heartbeatAt=now()`
- `advanceToNextPending()` — `currentStep=currentStep.nextPending()`, `status = currentStep==DONE ? DONE : PENDING`, `attempts=0`, `ownerInstanceId=null`, `heartbeatAt=null`
- `markFailed(Throwable e)` — **不再 `attempts++`**（已在 `claim` SQL 里 `attempts=attempts+1`，此处读当前值即可）；若 `attempts >= maxAttempts` → `status=FAILED`（`currentStep` 保持在 `RUNNING_*` 便于定位失败阶段），否则 `status=PENDING` 并把 `currentStep` 回滚到对应的 `PENDING_*`；`lastError=stack(e).substring(0, 2000)`
- `touchHeartbeat()` — `heartbeatAt=now()`
- `resetForManualRetry()` — 用于 retry-metadata 接口：`currentStep=PENDING_METADATA`, `status=PENDING`, `attempts=0`, `lastError=null`

- [ ] **Step 4：写单测**

`UploadTaskUnitTest`：covers 5 个领域行为方法 + 枚举跳转正确（PENDING_MERGE→RUNNING_MERGE→PENDING_INSPECT→...→DONE）。

- [ ] **Step 5：跑测试**

Run: `mvn -pl . test -Dtest=UploadTaskUnitTest`
Expected：全部通过。

- [ ] **Step 6：提交**

```bash
git add grace-platform/src/main/java/com/grace/platform/video/domain/UploadTask*.java \
        grace-platform/src/test/java/com/grace/platform/video/UploadTaskUnitTest.java
git commit -m "feat(video): UploadTask 聚合根与状态机枚举"
```

---

## Task 4: MyBatis Repository

**Files:**
- Create: `UploadTaskMapper.java`, `UploadTaskRepository.java`, `UploadTaskRepositoryImpl.java`
- Create: `grace-platform/src/main/resources/mapper/UploadTaskMapper.xml`
- Test: `grace-platform/src/test/java/com/grace/platform/video/infrastructure/UploadTaskRepositoryIntegrationTest.java`

- [ ] **Step 1：定义 Repository 接口**

```java
public interface UploadTaskRepository {
    UploadTask save(UploadTask task);          // INSERT or UPDATE
    Optional<UploadTask> findById(String id);
    Optional<UploadTask> findByVideoId(String videoId);
    /** 原子 claim：status PENDING→RUNNING；返回 1 = 成功 */
    int claim(String id, String instanceId);
    /** 心跳更新 */
    int touchHeartbeat(String id);
    /** 扫 PENDING 但超过 orphanSeconds 未被消费的任务 ID */
    List<String> findOrphanPendingIds(int orphanSeconds);
    /** 扫 RUNNING 但 heartbeat 超 zombieSeconds 的任务，重置为 PENDING（不 attempts++），返回受影响行数 */
    int resetZombies(int zombieSeconds);
}
```

- [ ] **Step 2：Mapper 接口与 XML**

MyBatis 注解风格沿用项目现有 `VideoMapper` 约定。关键 SQL（在 XML 里写）：

```xml
<update id="claim">
  UPDATE upload_task
     SET status='RUNNING',
         owner_instance_id=#{instanceId},
         heartbeat_at=NOW(),
         attempts=attempts+1,
         updated_at=NOW()
   WHERE id=#{id} AND status='PENDING'
</update>

<update id="resetZombies">
  UPDATE upload_task
     SET status='PENDING',
         owner_instance_id=NULL,
         heartbeat_at=NULL,
         updated_at=NOW()
   WHERE status='RUNNING'
     AND heartbeat_at &lt; DATE_SUB(NOW(), INTERVAL #{zombieSeconds} SECOND)
</update>

<select id="findOrphanPendingIds" resultType="string">
  SELECT id FROM upload_task
   WHERE status='PENDING'
     AND updated_at &lt; DATE_SUB(NOW(), INTERVAL #{orphanSeconds} SECOND)
   LIMIT 100
</select>
```

`payload` / `ext_info` 序列化：遵循 `VideoRepositoryImpl` 里对 `ext_info` 的做法（Jackson `ObjectMapper`）。

- [ ] **Step 3：写集成测试**

`UploadTaskRepositoryIntegrationTest` extends 项目现有的 `BaseIntegrationTest`（`grace-platform/src/test/java/com/grace/platform/testutil/`）。测试：

1. `save → findById` 能读回所有字段
2. `claim` 首次成功返回 1；重复 claim 返回 0
3. `resetZombies` 将 heartbeat 设为 3 分钟前的 RUNNING 行重置为 PENDING
4. `findOrphanPendingIds` 能找到 updated_at 在阈值外的 PENDING 行

- [ ] **Step 4：跑测试**

Run: `mvn -pl . test -Dtest=UploadTaskRepositoryIntegrationTest`
Expected：4 个测试全通过。

- [ ] **Step 5：提交**

```bash
git add grace-platform/src/main/java/com/grace/platform/video/{domain/UploadTaskRepository.java,infrastructure/persistence/UploadTask*} \
        grace-platform/src/main/resources/mapper/UploadTaskMapper.xml \
        grace-platform/src/test/java/com/grace/platform/video/infrastructure/UploadTaskRepositoryIntegrationTest.java
git commit -m "feat(video): UploadTaskRepository & MyBatis mapper"
```

---

## Task 5: `AsyncConfig` — 专用线程池 + `@EnableAsync` + `@EnableScheduling`

**Files:**
- Create: `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/config/AsyncConfig.java`
- Modify: `grace-platform/src/main/resources/application.yml`（新增 `grace.upload-task.*` 配置块）

- [ ] **Step 1：写 `AsyncConfig`**

```java
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    @Bean("uploadTaskExecutor")
    public ThreadPoolTaskExecutor uploadTaskExecutor(
            @Value("${grace.upload-task.executor.core-pool-size:4}") int core,
            @Value("${grace.upload-task.executor.max-pool-size:8}") int max,
            @Value("${grace.upload-task.executor.queue-capacity:50}") int queue) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(core);
        ex.setMaxPoolSize(max);
        ex.setQueueCapacity(queue);
        ex.setThreadNamePrefix("upload-task-");
        ex.setTaskDecorator(new MdcCopyingTaskDecorator()); // 沿用项目 EagleEye 约定，见 docs/backend/log-design.md
        ex.initialize();
        return ex;
    }
}
```

**注意**：`MdcCopyingTaskDecorator` 项目中若已存在（EagleEye 改造遗产）就复用；不存在的话本 Task 追加一个 20 行的实现，确保 traceId 能跨线程传递。检查路径：`grace-platform/src/main/java/com/grace/platform/shared/infrastructure/log/`。

- [ ] **Step 2：`application.yml` 追加**

```yaml
grace:
  upload-task:
    executor:
      core-pool-size: 4
      max-pool-size: 8
      queue-capacity: 50
    pull:
      scan-interval-ms: 30000
      orphan-threshold-s: 60
      zombie-threshold-s: 120
    max-attempts: 5
    instance-id: ${HOSTNAME:local}
```

- [ ] **Step 3：启动应用，日志搜 `upload-task-` 线程池创建日志**

Run: `mvn spring-boot:run` → `grep upload-task- logs/grace-platform/application.log`
Expected：出现 `Initializing ExecutorService 'uploadTaskExecutor'`。

- [ ] **Step 4：提交**

```bash
git add grace-platform/src/main/java/com/grace/platform/shared/infrastructure/config/AsyncConfig.java \
        grace-platform/src/main/resources/application.yml
git commit -m "feat(config): 异步任务专用线程池 & scheduler 使能"
```

---

## Task 6: `VideoUploadPipelineExecutor` — 核心驱动器

**Files:**
- Create: `grace-platform/src/main/java/com/grace/platform/video/application/VideoUploadPipelineExecutor.java`
- Create: `grace-platform/src/main/java/com/grace/platform/video/application/event/TaskReadyEvent.java`

- [ ] **Step 1：定义 `TaskReadyEvent`**

```java
public class TaskReadyEvent {
    private final String taskId;
    public TaskReadyEvent(String taskId) { this.taskId = taskId; }
    public String getTaskId() { return taskId; }
}
```

- [ ] **Step 2：写 executor 骨架**

类上 `@Service`。字段注入：
`UploadTaskRepository`, `VideoRepository`, `UploadSessionRepository`, `VideoMetadataRepository`, `ChunkMergeService`, `VideoFileInspector`, `StorageService`, `MetadataApplicationService`, `ApplicationEventPublisher`, `@Value("${grace.upload-task.instance-id}") String instanceId`。

核心 public 方法（每次只推进一步）：

```java
@Transactional(propagation = Propagation.NEVER)  // 每 step 自己开小事务
public void runOnce(String taskId) {
    int claimed = taskRepository.claim(taskId, instanceId);
    if (claimed == 0) { log.debug("claim miss {}", taskId); return; }
    UploadTask task = taskRepository.findById(taskId).orElseThrow();
    try {
        switch (task.getCurrentStep()) {
            case RUNNING_MERGE:    doMerge(task); break;
            case RUNNING_INSPECT:  doInspect(task); break;
            case RUNNING_OSS:      doOss(task); break;
            case RUNNING_METADATA: doMetadata(task); break;
            case RUNNING_CLEANUP:  doCleanup(task); break;
            default: throw new IllegalStateException("unexpected step " + task.getCurrentStep());
        }
        // 每个 doXxx 内部以独立事务推进 task 到 PENDING_{next}
        UploadTask refreshed = taskRepository.findById(taskId).orElseThrow();
        if (refreshed.getStatus() == UploadTaskStatus.PENDING) {
            eventPublisher.publishEvent(new TaskReadyEvent(taskId));  // 链式推进
        }
    } catch (Exception e) {
        failAndPersist(task, e);
    }
}
```

**注意 claim 后 `currentStep` 已是 `RUNNING_X`**（`markRunning` 在 claim 的 SQL 里没改，所以需要在 executor 入口处 `task.markRunning(instanceId)` 并立即 save）。实现时把 claim 拆成两段：
  1. `taskRepository.claim(id, instanceId)` — 只改 status/heartbeat/attempts（原子）
  2. `task.markRunning()` + `save()` — 推进 `currentStep` 到 `RUNNING_X`

- [ ] **Step 3：实现 `doMerge`**

```java
@Transactional
void doMerge(UploadTask task) {
    String uploadId = (String) task.getPayload().get("uploadId");
    UploadSession session = uploadSessionRepository.findById(uploadId).orElseThrow();
    Path tempDir = Paths.get(session.getTempDirectory());
    Path merged = tempDir.resolve("merged.bin");
    // 幂等：已合并且大小匹配 → 跳过
    if (!Files.exists(merged) || Files.size(merged) != session.getFileSize()) {
        chunkMergeService.merge(session, merged);
    }
    task.getPayload().put("mergedFilePath", merged.toString());
    task.advanceToNextPending();
    taskRepository.save(task);
}
```

- [ ] **Step 4：实现 `doInspect`**

```java
@Transactional
void doInspect(UploadTask task) {
    Path merged = Paths.get((String) task.getPayload().get("mergedFilePath"));
    VideoFileInfo info = videoFileInspector.inspect(merged, VideoFormat.valueOf((String) task.getPayload().get("format")));
    task.getPayload().put("durationSeconds", info.getDuration().getSeconds());
    // 同步更新 video.duration（幂等 UPDATE）
    Video v = videoRepository.findById(task.getVideoId()).orElseThrow();
    v.applyInspectedInfo(info);
    videoRepository.save(v);
    task.advanceToNextPending();
    taskRepository.save(task);
}
```

`Video.applyInspectedInfo` 是新增方法（聚合根）：设置 `duration` 字段；如有必要时也写 `fileSize`。

- [ ] **Step 5：实现 `doOss`**

```java
@Transactional
void doOss(UploadTask task) {
    Video v = videoRepository.findById(task.getVideoId()).orElseThrow();
    // 幂等：storageUrl 已非空且不是空 → 跳过
    if (v.getStorageUrl() == null || v.getStorageUrl().isBlank()) {
        Path merged = Paths.get((String) task.getPayload().get("mergedFilePath"));
        String objectKey = storageService.buildObjectKey(task.getPayload().get("uploadId").toString(), v.getFileName());
        storageService.uploadFile(merged, objectKey);
        String url = storageService.buildObjectUrl(objectKey);
        v.attachStorage(url, StorageProvider.OSS);
        videoRepository.save(v);
        task.getPayload().put("ossUrl", url);
    }
    task.advanceToNextPending();
    taskRepository.save(task);
}
```

- [ ] **Step 6：实现 `doMetadata`（含心跳守护线程）**

```java
@Transactional
void doMetadata(UploadTask task) {
    // 幂等：metadata 已存在 → 跳过 LLM
    if (videoMetadataRepository.findByVideoId(task.getVideoId()).isPresent()) {
        advanceAndMarkGenerated(task);
        return;
    }
    // 守护线程：LLM 预期 ~50s，每 30s touch 一次 heartbeat
    ScheduledExecutorService heartbeatExec = Executors.newSingleThreadScheduledExecutor();
    heartbeatExec.scheduleAtFixedRate(
        () -> taskRepository.touchHeartbeat(task.getId()),
        30, 30, TimeUnit.SECONDS);
    try {
        Video v = videoRepository.findById(task.getVideoId()).orElseThrow();
        metadataApplicationService.generateMetadata(task.getVideoId());   // 内部方法级事务
    } finally {
        heartbeatExec.shutdownNow();
    }
    advanceAndMarkGenerated(task);
}

private void advanceAndMarkGenerated(UploadTask task) {
    Video v = videoRepository.findById(task.getVideoId()).orElseThrow();
    if (v.getStatus() == VideoStatus.UPLOADED || v.getStatus() == VideoStatus.METADATA_FAILED) {
        v.transitionTo(VideoStatus.METADATA_GENERATED);
        videoRepository.save(v);
    }
    task.advanceToNextPending();
    taskRepository.save(task);
}
```

- [ ] **Step 7：实现 `doCleanup`**

```java
@Transactional
void doCleanup(UploadTask task) {
    Path merged = Paths.get((String) task.getPayload().get("mergedFilePath"));
    try { Files.deleteIfExists(merged); } catch (IOException ignored) {}
    Path tempDir = merged.getParent();
    // 递归删（chunk_* 已在 merge 后删，剩下基本只有 merged.bin 的父目录）
    if (Files.exists(tempDir)) FileUtils.deleteDirectory(tempDir.toFile());
    task.advanceToNextPending();   // 推到 DONE
    taskRepository.save(task);
}
```

- [ ] **Step 8：实现 `failAndPersist`**

```java
private void failAndPersist(UploadTask task, Exception e) {
    log.error("pipeline step failed taskId={} step={}", task.getId(), task.getCurrentStep(), e);
    task.markFailed(e);    // 内部决定转 PENDING 还是 FAILED
    taskRepository.save(task);
    // 如果是 METADATA 终态失败，同步推进 video.status = METADATA_FAILED
    if (task.getStatus() == UploadTaskStatus.FAILED && task.getCurrentStep() == UploadTaskStep.RUNNING_METADATA) {
        Video v = videoRepository.findById(task.getVideoId()).orElseThrow();
        if (v.getStatus() == VideoStatus.UPLOADED) {
            v.transitionTo(VideoStatus.METADATA_FAILED);
            videoRepository.save(v);
        }
    }
}
```

- [ ] **Step 9：提交**

```bash
git add grace-platform/src/main/java/com/grace/platform/video/application/VideoUploadPipelineExecutor.java \
        grace-platform/src/main/java/com/grace/platform/video/application/event/TaskReadyEvent.java
git commit -m "feat(video): VideoUploadPipelineExecutor 驱动 5 个大步骤"
```

---

## Task 7: Push 监听器

**Files:**
- Create: `grace-platform/src/main/java/com/grace/platform/video/application/UploadTaskPushListener.java`

- [ ] **Step 1：写监听器**

```java
@Component
public class UploadTaskPushListener {
    private final VideoUploadPipelineExecutor executor;
    public UploadTaskPushListener(VideoUploadPipelineExecutor executor) { this.executor = executor; }

    @Async("uploadTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(TaskReadyEvent event) {
        executor.runOnce(event.getTaskId());
    }
}
```

`fallbackExecution=true`：executor 内部链式 publish（非事务上下文）时也能触达。

- [ ] **Step 2：提交**

```bash
git add grace-platform/src/main/java/com/grace/platform/video/application/UploadTaskPushListener.java
git commit -m "feat(video): @Async Push 监听 TaskReadyEvent"
```

---

## Task 8: Pull Scheduler（兜底）

**Files:**
- Create: `grace-platform/src/main/java/com/grace/platform/video/application/UploadTaskPullScheduler.java`
- Test: 手写集成测试 `UploadTaskPullSchedulerIntegrationTest`

- [ ] **Step 1：写 scheduler**

```java
@Component
public class UploadTaskPullScheduler {
    private final UploadTaskRepository taskRepository;
    private final VideoUploadPipelineExecutor executor;
    @Value("${grace.upload-task.pull.orphan-threshold-s:60}") private int orphanSec;
    @Value("${grace.upload-task.pull.zombie-threshold-s:120}") private int zombieSec;

    @Scheduled(fixedDelayString = "${grace.upload-task.pull.scan-interval-ms:30000}")
    public void scan() {
        int reset = taskRepository.resetZombies(zombieSec);
        if (reset > 0) log.warn("pull scheduler reset {} zombies", reset);
        for (String id : taskRepository.findOrphanPendingIds(orphanSec)) {
            executor.runOnce(id);
        }
    }
}
```

- [ ] **Step 2：集成测试：构造一个 `updated_at` 为 2 分钟前的 PENDING task，验证 scheduler 触发 executor 跑完**

使用 `TestClock` 或直接 `@Autowired JdbcTemplate` 改 `updated_at`。测试断言：`DONE` 在 10s 内达到（for loop assertions with Thread.sleep）。

- [ ] **Step 3：提交**

```bash
git add grace-platform/src/main/java/com/grace/platform/video/application/UploadTaskPullScheduler.java \
        grace-platform/src/test/java/com/grace/platform/video/UploadTaskPullSchedulerIntegrationTest.java
git commit -m "feat(video): Pull scheduler 兜底扫僵尸 & 孤儿 task"
```

---

## Task 9: 改造 `ServerUploadApplicationService.completeUpload`

**Files:**
- Modify: `grace-platform/src/main/java/com/grace/platform/video/application/ServerUploadApplicationService.java`
- **Delete**: `grace-platform/src/main/java/com/grace/platform/metadata/application/listener/VideoUploadedEventListener.java`
- Modify: `grace-platform/src/main/java/com/grace/platform/metadata/application/MetadataApplicationService.java`（类级 @Transactional 改方法级）
- Modify: `grace-platform/src/main/java/com/grace/platform/video/application/dto/ServerUploadCompleteDTO.java`（新增 `taskId`）
- Modify: `grace-platform/src/main/java/com/grace/platform/video/interfaces/rest/dto/ServerUploadCompleteResponse.java`（新增 `taskId`）
- Modify: `grace-platform/src/main/java/com/grace/platform/video/interfaces/rest/ServerUploadController.java`（返回 202）

- [ ] **Step 1：删除 `VideoUploadedEventListener.java`**（已由 executor 驱动 LLM）

- [ ] **Step 2：改 `MetadataApplicationService`**：移除类级 `@Transactional`；把 `generateMetadata` 方法加 `@Transactional(propagation = REQUIRED)`。这样 executor 调用它时，若 executor 的 `doMetadata` 已经开了事务，会加入；若没有，自己起一个。

- [ ] **Step 3：改 `ServerUploadApplicationService.completeUpload`**

骨架（保留原校验逻辑，去掉所有合并/探测/OSS/LLM 调用）：

```java
@Transactional
public ServerUploadCompleteDTO completeUpload(String uploadId) {
    UploadSession session = uploadSessionRepository.findById(uploadId)
        .orElseThrow(...);
    validateActiveSession(session);
    validateAllChunksReceived(session);

    // 空壳 video（尚无 duration / storageUrl；在后续 step 中补）
    Video video = Video.createPendingProcessing(session);  // 新增工厂方法
    videoRepository.save(video);

    Map<String,Object> payload = new LinkedHashMap<>();
    payload.put("uploadId", uploadId);
    payload.put("fileName", session.getFileName());
    payload.put("fileSize", session.getFileSize());
    payload.put("format", session.getFormat().name());
    payload.put("tempDirectory", session.getTempDirectory());

    Map<String,Object> ext = new LinkedHashMap<>();
    ext.put(Video.EXT_TRACE_ID, TraceContext.traceId());
    ext.put(Video.EXT_CREATED_TRACE_ID, TraceContext.traceId());

    UploadTask task = UploadTask.createInitial(video.getId().getValue(), payload, ext);
    uploadTaskRepository.save(task);

    session.markAsCompleted();
    uploadSessionRepository.save(session);

    eventPublisher.publishEvent(new TaskReadyEvent(task.getId()));

    return ServerUploadCompleteDTO.builder()
        .videoId(video.getId().getValue())
        .taskId(task.getId())
        .fileName(video.getFileName())
        .fileSize(video.getFileSize())
        .format(video.getFormat().name())
        .status(video.getStatus().name())    // = "UPLOADED"
        .createdAt(video.getCreatedAt())
        .build();
}
```

- [ ] **Step 4：新增 `Video.createPendingProcessing`**

工厂方法：`status=UPLOADED`, `storageUrl=null`, `duration=null`, `storageProvider=OSS`（占位）。逻辑类似现有 `createWithStorageUrl` 但字段更少。

- [ ] **Step 5：`ServerUploadController` 返回 202**

```java
@PostMapping("/{uploadId}/complete")
public ResponseEntity<ServerUploadCompleteResponse> complete(@PathVariable String uploadId) {
    ServerUploadCompleteDTO dto = service.completeUpload(uploadId);
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(ServerUploadCompleteResponse.from(dto));
}
```

- [ ] **Step 6：改现有集成测试 `ServerUploadIntegrationTest`**（若存在）：`complete` 返回 202 + status=UPLOADED + task 在 60s 内变 DONE。如果原测试断言 `status=METADATA_GENERATED` 立刻返回，改为"轮询等待"。

- [ ] **Step 7：提交**

```bash
git add -A
git commit -m "feat(video): complete 接口改为异步，LLM 移至 pipeline executor"
```

---

## Task 10: `WebConfig.restTemplate()` 加超时

**Files:**
- Modify: `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/config/WebConfig.java`

- [ ] **Step 1：替换 `new RestTemplate()`**

```java
@Bean
public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(10_000);   // 10s
    f.setReadTimeout(120_000);     // 2min（覆盖 LLM 单次 p99）
    return new RestTemplate(f);
}
```

- [ ] **Step 2：提交**

```bash
git add grace-platform/src/main/java/com/grace/platform/shared/infrastructure/config/WebConfig.java
git commit -m "fix(http): RestTemplate 补 connect/read 超时防止无限 hang"
```

---

## Task 11: GET `/videos/{id}` 返回 `task` 字段

**Files:**
- Modify: `grace-platform/src/main/java/com/grace/platform/video/application/VideoApplicationService.java`（查询时 JOIN task）
- Modify: `grace-platform/src/main/java/com/grace/platform/video/interfaces/rest/dto/VideoDetailResponse.java`（+`TaskSummary task`）
- Modify: `grace-platform/src/main/java/com/grace/platform/video/interfaces/rest/VideoUploadController.java` 或对应的 `VideoQueryController`

- [ ] **Step 1：`TaskSummary` DTO**

```java
public record TaskSummary(String currentStep, String status, int attempts, String lastError) {}
```

- [ ] **Step 2：`VideoApplicationService.getDetail(videoId)`** 返回增强 DTO，内部 `uploadTaskRepository.findByVideoId` + map。

- [ ] **Step 3：接口返回 JSON 验证**

启动后 `curl localhost:8000/api/videos/<id>`，断言包含 `"task": { "currentStep": "...", ...}`。

- [ ] **Step 4：提交**

```bash
git add -A
git commit -m "feat(video): GET /videos/{id} 响应补 task 字段"
```

---

## Task 12: `POST /videos/{videoId}/retry-metadata`

**Files:**
- Modify: `VideoUploadController.java`（或新建 `VideoRetryController.java`，视现有风格）
- Modify: `VideoApplicationService.java`（+`retryMetadata(videoId)`）

- [ ] **Step 1：`VideoApplicationService.retryMetadata`**

```java
@Transactional
public RetryMetadataDTO retryMetadata(String videoId) {
    Video v = videoRepository.findById(new VideoId(videoId)).orElseThrow(...);
    if (v.getStatus() != VideoStatus.METADATA_FAILED) {
        throw new BusinessRuleViolationException(ErrorCode.INVALID_STATE, "video not in METADATA_FAILED");
    }
    UploadTask t = uploadTaskRepository.findByVideoId(videoId).orElseThrow(...);
    if (t.getStatus() == UploadTaskStatus.RUNNING) {
        throw new BusinessRuleViolationException(ErrorCode.ALREADY_RUNNING, "task already running");
    }
    t.resetForManualRetry();
    uploadTaskRepository.save(t);
    eventPublisher.publishEvent(new TaskReadyEvent(t.getId()));
    return new RetryMetadataDTO(t.getId(), t.getCurrentStep().name(), t.getStatus().name());
}
```

- [ ] **Step 2：Controller 端点**

```java
@PostMapping("/{videoId}/retry-metadata")
public ResponseEntity<RetryMetadataResponse> retryMetadata(@PathVariable String videoId) {
    return ResponseEntity.ok(RetryMetadataResponse.from(service.retryMetadata(videoId)));
}
```

- [ ] **Step 3：集成测试**

`VideoRetryIntegrationTest.retry_metadata_happy_path`：
1. 直接构造 video (METADATA_FAILED) + task (FAILED + PENDING_METADATA) 落库
2. `POST /videos/{id}/retry-metadata` → 200，task 变 PENDING
3. 等 ≤30s，mock 好的 LLM 返回合法 JSON → task DONE + video METADATA_GENERATED

- [ ] **Step 4：提交**

```bash
git add -A
git commit -m "feat(video): POST /videos/{id}/retry-metadata 手动重试接口"
```

---

## Task 13: 前端类型与 API

**Files:**
- Modify: `grace-frontend/src/types/video.ts`
- Modify: `grace-frontend/src/api/video.ts`

- [ ] **Step 1：`types/video.ts` 新增**

```ts
export type VideoStatus =
  | 'UPLOADED'
  | 'METADATA_GENERATED'
  | 'METADATA_FAILED'        // ← new
  | 'READY_TO_PUBLISH' | 'PUBLISHING' | 'PUBLISHED' | 'PUBLISH_FAILED' | 'PROMOTION_DONE';

export interface TaskSummary {
  currentStep: string;
  status: 'PENDING' | 'RUNNING' | 'FAILED' | 'DONE';
  attempts: number;
  lastError: string | null;
}

export interface VideoDetail {
  // ...existing fields
  task?: TaskSummary;
}

export interface ServerUploadCompleteResponse {
  videoId: string;
  taskId: string;           // ← new
  // ...existing
}
```

- [ ] **Step 2：`api/video.ts` 新增函数**

```ts
export function retryMetadata(videoId: string) {
  return apiClient.post<{ taskId: string; currentStep: string; status: string }>(
    `/videos/${videoId}/retry-metadata`
  );
}
```

- [ ] **Step 3：提交**

```bash
git add grace-frontend/src/types/video.ts grace-frontend/src/api/video.ts
git commit -m "feat(frontend): video 类型补 task/METADATA_FAILED + retryMetadata API"
```

---

## Task 14: `useServerUpload` 扩展 polling 终止条件

**Files:**
- Modify: `grace-frontend/src/hooks/useServerUpload.ts:60-77, 167-174`

- [ ] **Step 1：改 `pollVideoStatus`**

```ts
async function pollVideoStatus(videoId: string): Promise<VideoStatus> {
  for (let i = 0; i < MAX_POLL_ATTEMPTS; i++) {
    const v = await getVideo(videoId);
    if (v.status === 'METADATA_GENERATED' || v.status === 'METADATA_FAILED') {
      return v.status;
    }
    await sleep(POLL_INTERVAL);
  }
  throw new Error('轮询超时');
}
```

- [ ] **Step 2：主流程 `uploadFile` 末段在 `completed` 前判断**

```ts
const finalStatus = await pollVideoStatus(completeResult.videoId);
if (finalStatus === 'METADATA_FAILED') {
  setUploadState({ status: 'metadata_failed', videoId: completeResult.videoId, ... });
  return;  // 不抛异常，由 UI 显示重试按钮
}
setUploadState({ status: 'completed', ... });
```

- [ ] **Step 3：提交**

```bash
git add grace-frontend/src/hooks/useServerUpload.ts
git commit -m "feat(frontend): polling 支持 METADATA_FAILED 退出态"
```

---

## Task 15: 前端失败态 UI + 重试按钮

**Files:**
- Modify: `grace-frontend/src/pages/VideoUploadPage.tsx`
- 可能: `grace-frontend/src/components/ui/UploadProgressCard.tsx`（若存在）

- [ ] **Step 1：`UploadState` 新增 `metadata_failed` 分支**

在上传进度卡片下方渲染：

```tsx
{uploadState.status === 'metadata_failed' && (
  <div className="rounded-lg border border-red-300 bg-red-50 p-3 text-sm text-red-900">
    <div>元数据生成失败，视频已上传但待补齐。</div>
    <button
      className="mt-2 rounded bg-red-600 px-3 py-1 text-white hover:bg-red-700"
      onClick={async () => {
        await retryMetadata(uploadState.videoId);
        toast.success('已提交重试，稍候刷新');
        // 重新开始轮询
        const s = await pollVideoStatus(uploadState.videoId);
        if (s === 'METADATA_GENERATED') setUploadState({ status: 'completed', ... });
      }}
    >重试元数据生成</button>
  </div>
)}
```

- [ ] **Step 2：E2E 起个 happy path**（vite dev + 后端）

手动：上传一个小 MP4 → 看到 "处理中..." → 自动变"完成"。断网 mock LLM 错误再测重试按钮。

- [ ] **Step 3：提交**

```bash
git add grace-frontend/src/pages/VideoUploadPage.tsx
git commit -m "feat(frontend): 元数据失败态 UI 与重试按钮"
```

---

## Task 16: 文档同步（硬约束，见 `docs/lessons.md` L1）

**Files:**
- Modify: `docs/backend/db-design.md`（已在 Task 1 同步）
- Modify: `api.md`
- Modify: `architecture.md`
- Modify: `docs/video_upload.md`

- [ ] **Step 1：`api.md`**

在 "视频" 分区追加：
1. `POST /api/videos/upload/server/{uploadId}/complete` 的响应改为 202 + 补 `taskId` 字段
2. 新增 `POST /api/videos/{videoId}/retry-metadata`
3. `GET /api/videos/{videoId}` 响应补 `task` 字段

- [ ] **Step 2：`architecture.md`** 更新数据流图（参考本 plan 顶部的 spec）。

- [ ] **Step 3：`docs/video_upload.md`** 末尾追加 "## v2 异步链路（2026-05）" 章节，引用 spec 链接，并指出原 §1-§8 为 v1 基线。

- [ ] **Step 4：提交**

```bash
git add docs/
git commit -m "docs: 同步 api/architecture/video_upload 到异步链路 v2"
```

---

## Task 17: 验收 — 可重现的成功判定

对齐 spec §10 的 5 条 goal-driven 判定。按顺序执行，全部达成才算完成。

- [ ] **Step 1：Happy path 性能**

操作：前端传一个 2MB 小视频 → 观察 DevTools Network。
断言：
- `/complete` HTTP 状态 202
- `/complete` 响应时间 ≤ 500ms
- 前端在 ≤ 60s 内进入 `completed` 态

- [ ] **Step 2：可恢复实验 A — 进程中途 kill**

```bash
# 1. 前端发起上传
# 2. 观察后端日志，在 "RUNNING_OSS" 出现后立刻：
kill -9 $(cat .pids/backend.pid)
# 3. 重启
bash start.sh
# 4. 观察 150s 内 SELECT * FROM upload_task WHERE video_id=... → status=DONE
```

Expected：`video.status=METADATA_GENERATED`。

- [ ] **Step 3：可恢复实验 B — Mock LLM 前 4 次失败**

临时改 `QwenLlmServiceAdapter` 加环境变量开关 `GRACE_LLM_FAIL_FIRST_N`，回归时复原。上传视频后观察 task 日志：attempts 从 1 递增到 5，最终 `DONE`。

- [ ] **Step 4：终态失败实验**

`GRACE_LLM_FAIL_FIRST_N=10` → 上传 → video `METADATA_FAILED`，前端显示重试按钮。
点击重试 → 清环境变量 → 观察 ≤ 60s 后 video `METADATA_GENERATED`。

- [ ] **Step 5：日志链路连续性**

```bash
grep <traceId> logs/grace-platform/application.log | wc -l
```
Expected：同 traceId 覆盖 `complete` → `TaskReadyEvent` 派发 → 5 个 step 全程日志。Push/Pull 两条路径各至少有一条。

- [ ] **Step 6：最终提交**

```bash
# 把所有验收过程中的小修小补一并提交
git add -A
git commit -m "chore: 异步视频上传链路 v2 验收通过"
```

---

## 执行完成后的交付物

1. `upload_task` 表 + 2 个 worker（Push/Pull） + 1 个 executor
2. 新接口 `POST /videos/{id}/retry-metadata`
3. `complete` 接口 P99 ≤ 500ms
4. 重启 / LLM 失败 / 终态失败 3 个场景可复现且都能最终一致
5. `db-design.md / api.md / architecture.md / video_upload.md` 与代码一致
