# Phase 4: Distribution 限界上下文

> 参考实施路线图：`docs/backend/00-index.md` §6 Phase 3
> 产出：Distribution 上下文全部领域模型、Strategy+Registry、OAuth、YouTube 适配器、持久层、测试

## 进度统计

- [ ] 共 12 个任务，已完成 1/12

---

## 任务列表

### P4-01: 创建 Distribution 枚举与值对象

- **参考文档**: `docs/backend/05-context-distribution.md` §B.3（PublishStatus 枚举）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/distribution/domain/PublishStatus.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/domain/PublishResult.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/domain/UploadStatus.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-02
- **状态**: [x]
- **注意**: PublishStatus 有 5 个状态（PENDING/UPLOADING/COMPLETED/FAILED/QUOTA_EXCEEDED）；PublishResult 和 UploadStatus 为 record 值对象

---

### P4-02: 创建 PublishRecord 与 OAuthToken 实体

- **参考文档**: `docs/backend/05-context-distribution.md` §B.1（PublishRecord 字段表 + 状态机）、§B.2（OAuthToken 字段表 + isExpired 方法）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/distribution/domain/PublishRecord.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/domain/OAuthToken.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P4-01, P1-04
- **状态**: [x]
- **注意**: OAuthToken 的 accessToken/refreshToken 为加密存储字段；PublishRecord 含 retryCount 用于配额重试

---

### P4-03: 创建 Strategy 接口与 Registry

- **参考文档**: `docs/backend/05-context-distribution.md` §C.1.1（VideoDistributor 接口代码）、§C.1.2（ResumableVideoDistributor 扩展接口）、§C.1.3（VideoDistributorRegistry 完整代码）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/distribution/domain/VideoDistributor.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/domain/ResumableVideoDistributor.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/domain/VideoDistributorRegistry.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P4-01, P1-08
- **状态**: [x]
- **注意**: VideoDistributorRegistry 构造器接收 List<VideoDistributor>，通过 Stream 转为 Map<platform, distributor>；getDistributor 未找到时抛 3001

---

### P4-04: 创建 Distribution 领域接口与事件

- **参考文档**: `docs/backend/05-context-distribution.md` §C.3（OAuthService 接口代码含 3 个方法）、§C.4（VideoPublishedEvent 代码）、§D.1（PublishRecordRepository 方法签名表）、§D.2（OAuthTokenRepository 方法签名表）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/distribution/domain/OAuthService.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/domain/PublishRecordRepository.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/domain/OAuthTokenRepository.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/domain/event/VideoPublishedEvent.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P4-02, P1-06
- **状态**: [ ]

---

### P4-05: 创建 Distribution 应用层

- **参考文档**: `docs/backend/05-context-distribution.md` §E.1（DistributionApplicationService 6 个方法编排逻辑表）、§E.2（publish 时序图）；`api.md` §D（D1-D6 契约）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/distribution/application/DistributionApplicationService.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/application/command/PublishCommand.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/application/dto/PublishResultDTO.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/application/dto/UploadStatusDTO.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/application/dto/PlatformInfoDTO.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/application/dto/PublishRecordDTO.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/application/listener/MetadataConfirmedEventListener.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P4-03, P4-04, P2-04, P3-02, P1-07
- **状态**: [ ]

---

### P4-06: 创建 QuotaRetryScheduler

- **参考文档**: `docs/backend/05-context-distribution.md` §E.3（QuotaRetryScheduler 代码骨架 + 重试策略表：30分钟间隔、最大5次）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/distribution/application/scheduler/QuotaRetryScheduler.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P4-04, P4-03
- **状态**: [ ]
- **注意**: @Scheduled(fixedDelayString)；查询 QUOTA_EXCEEDED → 尝试 resumeUpload → 成功则 COMPLETED，仍超限则 retryCount++，>=5 则 FAILED

---

### P4-07: 创建 DistributionController

- **参考文档**: `docs/backend/05-context-distribution.md` §F.1（端点映射表 D1-D6）、§F.2（Request/Response DTO 字段）；`api.md` §D
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/distribution/interfaces/DistributionController.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/interfaces/dto/request/PublishRequest.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/interfaces/dto/response/PublishResultResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/interfaces/dto/response/UploadStatusResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/interfaces/dto/response/PlatformInfoResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/interfaces/dto/response/AuthUrlResponse.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P4-05, P1-07, P1-09
- **状态**: [ ]

---

### P4-08: 创建 YouTube 适配器

- **参考文档**: `docs/backend/05-context-distribution.md` §G.1（YouTubeDistributor 代码骨架）、§G.2（YouTubeApiAdapter 接口签名 + API 配置表）、§G.3（YouTubeOAuthServiceImpl + OAuth 流程时序图）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/distribution/infrastructure/youtube/YouTubeDistributor.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/infrastructure/youtube/YouTubeApiAdapter.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/infrastructure/youtube/YouTubeApiAdapterImpl.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/infrastructure/youtube/YouTubeOAuthServiceImpl.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P4-03, P4-04, P1-11
- **状态**: [ ]
- **注意**: YouTubeOAuthServiceImpl 实现 OAuthService 接口；Token 使用 EncryptionService 加密存储；YouTube API 配额每日 10000 units

---

### P4-09: 创建 Distribution MyBatis Mapper + Repository 实现

- **参考文档**: `docs/backend/05-context-distribution.md` §G.4（PublishRecordMapper/OAuthTokenMapper 接口代码 + 数据库列映射表）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/distribution/infrastructure/persistence/PublishRecordMapper.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/infrastructure/persistence/OAuthTokenMapper.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/infrastructure/persistence/PublishRecordRepositoryImpl.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/infrastructure/persistence/OAuthTokenRepositoryImpl.java`
  - `grace-platform/src/main/resources/mapper/distribution/PublishRecordMapper.xml`
  - `grace-platform/src/main/resources/mapper/distribution/OAuthTokenMapper.xml`
- **验证命令**: `mvn clean compile`
- **依赖**: P4-02, P1-05
- **状态**: [ ]
- **注意**: OAuthTokenRepositoryImpl 读取时需调 EncryptionService.decrypt()，存储时调 encrypt()

---

### P4-10: 创建 Distribution 配置类

- **参考文档**: `docs/backend/05-context-distribution.md` §C.1.3（DistributionConfig @Bean 代码）；`docs/backend/09-infrastructure-config.md`（YouTube 配置项）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/distribution/infrastructure/config/DistributionConfig.java`
  - `grace-platform/src/main/java/com/grace/platform/distribution/infrastructure/config/YouTubeProperties.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P4-03
- **状态**: [ ]

---

### P4-11: 创建 Distribution 属性测试

- **参考文档**: `docs/backend/10-testing-strategy.md` §B（Correctness Properties #6-#8：平台路由正确性、发布结果含有效 URL、发布记录持久化往返）
- **产出文件**:
  - `grace-platform/src/test/java/com/grace/platform/distribution/DistributionPropertyTest.java`
- **验证命令**: `mvn test -Dtest="DistributionPropertyTest"`
- **依赖**: P4-02, P4-03, P1-16
- **状态**: [ ]
- **测试要点**: Property #6 Registry 路由解析正确性；Property #7 发布成功时 videoUrl 非空且格式合法；Property #8 PublishRecord 持久化往返

---

### P4-12: 创建 Distribution 单元测试

- **参考文档**: `docs/backend/10-testing-strategy.md` §C（单元测试：平台路由错误、OAuth Token 刷新、配额处理）
- **产出文件**:
  - `grace-platform/src/test/java/com/grace/platform/distribution/DistributionUnitTest.java`
- **验证命令**: `mvn test -Dtest="DistributionUnitTest"`
- **依赖**: P4-03, P4-08, P1-16
- **状态**: [ ]
- **测试要点**: 不支持的平台抛 3001；Token 过期时自动刷新；配额超限标记 QUOTA_EXCEEDED
