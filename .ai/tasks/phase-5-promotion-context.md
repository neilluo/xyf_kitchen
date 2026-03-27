# Phase 5: Promotion 限界上下文

> 参考实施路线图：`docs/backend/00-index.md` §6 Phase 4
> 产出：Promotion 上下文全部领域模型、Strategy+Registry、OpenCrawl 适配器、LLM 文案生成、持久层、测试

## 进度统计

- [ ] 共 12 个任务，已完成 1/12

---

## 任务列表

### P5-01: 创建 Promotion 枚举与值对象

- **参考文档**: `docs/backend/06-context-promotion.md` §B.3（PromotionCopy/PromotionReport/ChannelExecutionSummary record 代码）、§B.4（ChannelType/ChannelStatus/PromotionMethod/PromotionStatus 枚举）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/ChannelType.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/ChannelStatus.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/PromotionMethod.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/PromotionStatus.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/PromotionResult.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/vo/PromotionCopy.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/vo/PromotionReport.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/vo/ChannelExecutionSummary.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-04
- **状态**: [x]

---

### P5-02: 创建 PromotionChannel 与 PromotionRecord 实体

- **参考文档**: `docs/backend/06-context-promotion.md` §B.1（PromotionChannel 字段表 + setApiKey/getDecryptedApiKey/enable/disable 方法代码）、§B.2（PromotionRecord 字段表 + 状态机）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/PromotionChannel.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/PromotionRecord.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P5-01, P1-04, P1-11
- **状态**: [x]
- **注意**: PromotionChannel.setApiKey 接收 EncryptionService 进行加密；priority 范围 1-99

---

### P5-03: 创建 Promotion Strategy 接口与 Registry

- **参考文档**: `docs/backend/06-context-promotion.md` §C.1.1（PromotionExecutor 接口 + PromotionResult record 代码）、§C.1.2（PromotionExecutorRegistry 完整代码）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/PromotionExecutor.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/PromotionExecutorRegistry.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P5-01, P1-08
- **状态**: [ ]

---

### P5-04: 创建 Promotion 领域接口

- **参考文档**: `docs/backend/06-context-promotion.md` §C.2（PromotionCopyGenerationService 接口代码）、§D.1（PromotionChannelRepository 方法签名表）、§D.2（PromotionRecordRepository 方法签名表）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/PromotionCopyGenerationService.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/PromotionChannelRepository.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/domain/PromotionRecordRepository.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P5-02
- **状态**: [ ]

---

### P5-05: 创建 Channel 应用层

- **参考文档**: `docs/backend/06-context-promotion.md` §E.1（ChannelApplicationService 5 个方法编排逻辑表）；`api.md` §E（E1-E5 契约）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/promotion/application/ChannelApplicationService.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/application/command/CreateChannelCommand.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/application/command/UpdateChannelCommand.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/application/dto/ChannelDTO.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P5-04, P1-07, P1-11
- **状态**: [ ]
- **注意**: deleteChannel 逻辑：有关联推广记录则 disable（软删除），无则硬删除

---

### P5-06: 创建 Promotion 应用层

- **参考文档**: `docs/backend/06-context-promotion.md` §E.2（PromotionApplicationService 5 个方法编排逻辑表）、§E.3（executePromotion 时序图）、§C.3（批量执行策略：按 priority 排序、单失败不中断）；`api.md` §F（F1-F5 契约）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/promotion/application/PromotionApplicationService.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/application/command/ExecutePromotionCommand.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/application/dto/PromotionCopyDTO.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/application/dto/PromotionResultDTO.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/application/dto/PromotionRecordDTO.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/application/dto/PromotionReportDTO.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P5-03, P5-04, P5-05, P1-07
- **状态**: [ ]

---

### P5-07: 创建 ChannelController 与 PromotionController

- **参考文档**: `docs/backend/06-context-promotion.md` §F.1（ChannelController 端点表 E1-E5）、§F.2（PromotionController 端点表 F1-F5）、§F.3（Request/Response DTO 字段表）；`api.md` §E, §F
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/promotion/interfaces/ChannelController.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/interfaces/PromotionController.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/interfaces/dto/request/CreateChannelRequest.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/interfaces/dto/request/UpdateChannelRequest.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/interfaces/dto/request/ExecutePromotionRequest.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/interfaces/dto/request/GenerateCopyRequest.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/interfaces/dto/response/ChannelResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/interfaces/dto/response/PromotionCopyResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/interfaces/dto/response/PromotionResultResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/interfaces/dto/response/PromotionRecordResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/interfaces/dto/response/PromotionReportResponse.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P5-05, P5-06, P1-07, P1-09
- **状态**: [ ]

---

### P5-08: 创建 OpenCrawl 适配器

- **参考文档**: `docs/backend/06-context-promotion.md` §G.1（OpenCrawlPromotionExecutor 代码骨架）、§G.2（OpenCrawlAdapter 接口）；`docs/backend/09-infrastructure-config.md`（OpenCrawl 配置项）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/promotion/infrastructure/opencrawl/OpenCrawlPromotionExecutor.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/infrastructure/opencrawl/OpenCrawlAdapter.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/infrastructure/opencrawl/OpenCrawlAdapterImpl.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/infrastructure/opencrawl/OpenCrawlRequest.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/infrastructure/opencrawl/OpenCrawlResponse.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P5-03, P5-02, P1-11
- **状态**: [ ]

---

### P5-09: 创建 PromotionCopyGenerationServiceImpl

- **参考文档**: `docs/backend/06-context-promotion.md` §G.3（PromotionCopyGenerationServiceImpl 代码骨架 + Prompt 模板表按 ChannelType 区分）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/promotion/infrastructure/llm/PromotionCopyGenerationServiceImpl.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P5-04, P3-03
- **状态**: [ ]
- **注意**: 复用 Metadata 上下文的 LlmService；4 种 ChannelType 各有不同的 Prompt 风格（SOCIAL_MEDIA≤280字符、FORUM 结构化、BLOG 引流、OTHER 通用）

---

### P5-10: 创建 Promotion MyBatis Mapper + Repository 实现

- **参考文档**: `docs/backend/06-context-promotion.md` §G.4（PromotionChannelMapper/PromotionRecordMapper 接口代码）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/promotion/infrastructure/persistence/PromotionChannelMapper.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/infrastructure/persistence/PromotionRecordMapper.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/infrastructure/persistence/PromotionChannelRepositoryImpl.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/infrastructure/persistence/PromotionRecordRepositoryImpl.java`
  - `grace-platform/src/main/resources/mapper/promotion/PromotionChannelMapper.xml`
  - `grace-platform/src/main/resources/mapper/promotion/PromotionRecordMapper.xml`
- **验证命令**: `mvn clean compile`
- **依赖**: P5-02, P1-05
- **状态**: [ ]

---

### P5-11: 创建 Promotion 配置类

- **参考文档**: `docs/backend/06-context-promotion.md` §C.1.2（PromotionConfig @Bean 代码）；`docs/backend/09-infrastructure-config.md`（OpenCrawl 配置项）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/promotion/infrastructure/config/PromotionConfig.java`
  - `grace-platform/src/main/java/com/grace/platform/promotion/infrastructure/config/OpenCrawlProperties.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P5-03
- **状态**: [ ]

---

### P5-12: 创建 Promotion 属性测试与单元测试

- **参考文档**: `docs/backend/10-testing-strategy.md` §B（Correctness Properties #9-#12：Channel CRUD 往返、API Key 加密存储、推广文案结构不变量、推广记录持久化）、§C（单元测试：Channel 软硬删除、批量推广失败隔离）
- **产出文件**:
  - `grace-platform/src/test/java/com/grace/platform/promotion/PromotionPropertyTest.java`
  - `grace-platform/src/test/java/com/grace/platform/promotion/PromotionUnitTest.java`
- **验证命令**: `mvn test -Dtest="PromotionPropertyTest,PromotionUnitTest"`
- **依赖**: P5-02, P5-03, P1-16
- **状态**: [ ]
- **测试要点**: Property #9 Channel 创建→读取→更新→删除往返；Property #11 推广文案含 title+body+method；单元测试：有记录的 Channel 执行 disable 而非硬删；单个渠道失败不中断批量执行
