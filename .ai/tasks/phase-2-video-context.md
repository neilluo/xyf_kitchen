# Phase 2: Video 限界上下文

> 参考实施路线图：`docs/backend/00-index.md` §6 Phase 2
> 产出：Video 上下文全部领域模型、应用服务、REST API、持久层、测试

## 进度统计

- [x] 共 12 个任务，已完成 2/12

---

## 任务列表

### P2-01: 创建 Video 枚举与值对象

- **参考文档**: `docs/backend/03-context-video.md` §B.3（VideoFormat/VideoStatus/UploadSessionStatus 枚举 + VideoFileInfo 值对象）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/video/domain/VideoFormat.java`
  - `grace-platform/src/main/java/com/grace/platform/video/domain/VideoStatus.java`
  - `grace-platform/src/main/java/com/grace/platform/video/domain/UploadSessionStatus.java`
  - `grace-platform/src/main/java/com/grace/platform/video/domain/VideoFileInfo.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-02
- **状态**: [x]
- **注意**: VideoStatus 有 7 个状态（UPLOADED → METADATA_GENERATED → READY_TO_PUBLISH → PUBLISHING → PUBLISHED → PUBLISH_FAILED → PROMOTION_DONE），VideoFormat 为 MP4/MOV/AVI/MKV

---

### P2-02: 创建 Video 聚合根

- **参考文档**: `docs/backend/03-context-video.md` §B.1（Video 字段列表、状态机逻辑）、§B.4（校验规则表）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/video/domain/Video.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P2-01, P1-04
- **状态**: [x]
- **注意**: 状态转换方法需校验当前状态→目标状态合法性；字段含 id(VideoId)/fileName/fileSize/format/duration/filePath/status/createdAt/updatedAt

---

### P2-03: 创建 UploadSession 实体

- **参考文档**: `docs/backend/03-context-video.md` §B.2（UploadSession 字段列表）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/video/domain/UploadSession.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P2-01
- **状态**: [ ]
- **注意**: uploadId 使用 "upl_" 前缀 + UUID；totalChunks = ceil(fileSize / chunkSize)；含过期判断逻辑

---

### P2-04: 创建 Video 领域接口

- **参考文档**: `docs/backend/03-context-video.md` §C.1（VideoFileInspector 接口代码）、§C.2（ChunkMergeService 接口代码）、§D.1（VideoRepository 方法签名）、§D.2（UploadSessionRepository 方法签名）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/video/domain/VideoFileInspector.java`
  - `grace-platform/src/main/java/com/grace/platform/video/domain/ChunkMergeService.java`
  - `grace-platform/src/main/java/com/grace/platform/video/domain/VideoRepository.java`
  - `grace-platform/src/main/java/com/grace/platform/video/domain/UploadSessionRepository.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P2-02, P2-03
- **状态**: [ ]

---

### P2-05: 创建 VideoUploadedEvent 领域事件

- **参考文档**: `docs/backend/03-context-video.md` §C.3（VideoUploadedEvent 代码，含 videoId/fileName/fileSize/format 字段）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/video/domain/event/VideoUploadedEvent.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-06, P1-04
- **状态**: [ ]

---

### P2-06: 创建 Video 应用层

- **参考文档**: `docs/backend/03-context-video.md` §E.1（VideoApplicationService 6 个方法签名与编排逻辑）、§E.2（completeUpload 时序图）；`api.md` §B（B1-B6 请求/响应契约）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/video/application/VideoApplicationService.java`
  - `grace-platform/src/main/java/com/grace/platform/video/application/command/UploadInitCommand.java`
  - `grace-platform/src/main/java/com/grace/platform/video/application/command/VideoQueryCommand.java`
  - `grace-platform/src/main/java/com/grace/platform/video/application/dto/UploadInitDTO.java`
  - `grace-platform/src/main/java/com/grace/platform/video/application/dto/ChunkUploadDTO.java`
  - `grace-platform/src/main/java/com/grace/platform/video/application/dto/UploadProgressDTO.java`
  - `grace-platform/src/main/java/com/grace/platform/video/application/dto/VideoInfoDTO.java`
  - `grace-platform/src/main/java/com/grace/platform/video/application/dto/VideoListItemDTO.java`
  - `grace-platform/src/main/java/com/grace/platform/video/application/dto/VideoDetailDTO.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P2-04, P2-05, P1-07
- **状态**: [ ]

---

### P2-07: 创建 VideoUploadController

- **参考文档**: `docs/backend/03-context-video.md` §F.1（端点映射表）、§F.2（Request/Response DTO 字段）；`api.md` §B（B1-B6 完整契约）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/video/interfaces/VideoUploadController.java`
  - `grace-platform/src/main/java/com/grace/platform/video/interfaces/dto/request/UploadInitRequest.java`
  - `grace-platform/src/main/java/com/grace/platform/video/interfaces/dto/response/UploadInitResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/video/interfaces/dto/response/VideoInfoResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/video/interfaces/dto/response/VideoDetailResponse.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P2-06, P1-07, P1-09
- **状态**: [ ]

---

### P2-08: 创建 Video MyBatis Mapper 接口与 XML

- **参考文档**: `docs/backend/03-context-video.md` §G.1（数据库列映射表）、§G.2（VideoMapper 接口 + ResultMap + 动态 SQL）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/video/infrastructure/persistence/VideoMapper.java`
  - `grace-platform/src/main/java/com/grace/platform/video/infrastructure/persistence/UploadSessionMapper.java`
  - `grace-platform/src/main/resources/mapper/video/VideoMapper.xml`
  - `grace-platform/src/main/resources/mapper/video/UploadSessionMapper.xml`
- **验证命令**: `mvn clean compile`
- **依赖**: P2-02, P2-03, P1-05
- **状态**: [ ]
- **注意**: duration 字段在 DB 为 `duration_seconds` BIGINT，领域对象为 `Duration`，需在 RepositoryImpl 中转换

---

### P2-09: 创建 Video Repository 实现

- **参考文档**: `docs/backend/03-context-video.md` §G.3（VideoRepositoryImpl 代码骨架）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/video/infrastructure/persistence/VideoRepositoryImpl.java`
  - `grace-platform/src/main/java/com/grace/platform/video/infrastructure/persistence/UploadSessionRepositoryImpl.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P2-08
- **状态**: [ ]

---

### P2-10: 创建 Video 基础设施文件服务实现

- **参考文档**: `docs/backend/03-context-video.md` §G.4（VideoFileInspectorImpl + ChunkMergeServiceImpl 代码骨架）、§G.5（文件存储路径约定表）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/video/infrastructure/file/VideoFileInspectorImpl.java`
  - `grace-platform/src/main/java/com/grace/platform/video/infrastructure/file/ChunkMergeServiceImpl.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P2-04
- **状态**: [ ]
- **注意**: VideoFileInspectorImpl 使用 ProcessBuilder 调用 ffprobe；ChunkMergeServiceImpl 使用 FileChannel 合并

---

### P2-11: 创建 Video 属性测试

- **参考文档**: `docs/backend/10-testing-strategy.md` §B（Correctness Properties #1-#3：文件信息提取、格式校验边界、持久化往返）
- **产出文件**:
  - `grace-platform/src/test/java/com/grace/platform/video/VideoPropertyTest.java`
- **验证命令**: `mvn test -Dtest="VideoPropertyTest"`
- **依赖**: P2-02, P1-16
- **状态**: [ ]
- **测试要点**: Property #1 视频文件信息提取准确性、Property #2 格式白名单边界、Property #3 Video 持久化往返一致性；每个 Property ≥100 次迭代

---

### P2-12: 创建 Video 单元测试

- **参考文档**: `docs/backend/10-testing-strategy.md` §C（单元测试列表：格式校验错误、文件大小边界、分片索引校验）
- **产出文件**:
  - `grace-platform/src/test/java/com/grace/platform/video/VideoUnitTest.java`
- **验证命令**: `mvn test -Dtest="VideoUnitTest"`
- **依赖**: P2-02, P2-03, P1-16
- **状态**: [ ]
- **测试要点**: 无效格式抛出 1001、文件超 5GB 抛出 1002、分片索引越界抛出 1005、重复分片抛出 1006、未完成调 complete 抛出 1007
