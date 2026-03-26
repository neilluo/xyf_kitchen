# Phase 3: Metadata 限界上下文

> 参考实施路线图：`docs/backend/00-index.md` §6 Phase 2
> 产出：Metadata 上下文全部领域模型、LLM 适配器、应用服务、REST API、持久层、测试

## 进度统计

- [ ] 共 10 个任务，已完成 0/10

---

## 任务列表

### P3-01: 创建 Metadata 枚举与域实体

- **参考文档**: `docs/backend/04-context-metadata.md` §B.1（VideoMetadata 聚合根字段 + validate/update/confirm 方法代码）、§B.2（MetadataSource 枚举）、§B.3（领域不变量表）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/metadata/domain/MetadataSource.java`
  - `grace-platform/src/main/java/com/grace/platform/metadata/domain/VideoMetadata.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-04, P1-08
- **状态**: [ ]
- **注意**: VideoMetadata 含 validate()（标题≤100、描述≤5000、标签5-15个）、update()（已确认不可编辑）、confirm()（设 confirmed=true 后不可逆）

---

### P3-02: 创建 Metadata 领域接口与事件

- **参考文档**: `docs/backend/04-context-metadata.md` §C.1（MetadataGenerationService 接口代码）、§C.3（MetadataConfirmedEvent 代码）、§D.1（VideoMetadataRepository 方法签名）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/metadata/domain/MetadataGenerationService.java`
  - `grace-platform/src/main/java/com/grace/platform/metadata/domain/VideoMetadataRepository.java`
  - `grace-platform/src/main/java/com/grace/platform/metadata/domain/event/MetadataConfirmedEvent.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P3-01, P1-06
- **状态**: [ ]

---

### P3-03: 创建 LLM 基础设施（LlmService + QwenAdapter）

- **参考文档**: `docs/backend/04-context-metadata.md` §G.1.1（LlmService 接口 + LlmRequest/LlmResponse record 代码）、§G.1.2（QwenLlmServiceAdapter 骨架 + 重试策略）；`docs/backend/09-infrastructure-config.md`（Qwen 配置项）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/llm/LlmService.java`
  - `grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/llm/LlmRequest.java`
  - `grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/llm/LlmResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/llm/QwenLlmServiceAdapter.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-08
- **状态**: [ ]
- **注意**: QwenLlmServiceAdapter 需实现指数退避重试（1s/2s/4s，最多3次），失败抛 ExternalServiceException(9001)

---

### P3-04: 创建 MetadataGenerationServiceImpl

- **参考文档**: `docs/backend/04-context-metadata.md` §G.1.3（MetadataGenerationServiceImpl 代码骨架 + Prompt 模板表 + LLM 响应解析映射）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/llm/MetadataGenerationServiceImpl.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P3-02, P3-03
- **状态**: [ ]
- **注意**: systemPrompt 为美食视频元数据专家角色；userPrompt 包含 fileName + 历史标题风格；temperature=0.7, maxTokens=2048

---

### P3-05: 创建 Metadata 应用层

- **参考文档**: `docs/backend/04-context-metadata.md` §E.1（MetadataApplicationService 5 个方法编排逻辑）、§E.2（generateMetadata 时序图）、§E.3（confirmMetadata 时序图）、§C.2（VideoUploadedEventListener 代码）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/metadata/application/MetadataApplicationService.java`
  - `grace-platform/src/main/java/com/grace/platform/metadata/application/command/UpdateMetadataCommand.java`
  - `grace-platform/src/main/java/com/grace/platform/metadata/application/dto/VideoMetadataDTO.java`
  - `grace-platform/src/main/java/com/grace/platform/metadata/application/listener/VideoUploadedEventListener.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P3-02, P3-04, P2-04, P1-07
- **状态**: [ ]
- **注意**: generateMetadata 由 VideoUploadedEventListener 自动触发；confirmMetadata 需更新 Video 状态为 READY_TO_PUBLISH 并发布 MetadataConfirmedEvent

---

### P3-06: 创建 MetadataController

- **参考文档**: `docs/backend/04-context-metadata.md` §F.1（端点映射表 C1-C5）、§F.2（Request/Response DTO 字段）；`api.md` §C
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/metadata/interfaces/MetadataController.java`
  - `grace-platform/src/main/java/com/grace/platform/metadata/interfaces/dto/request/GenerateMetadataRequest.java`
  - `grace-platform/src/main/java/com/grace/platform/metadata/interfaces/dto/request/UpdateMetadataRequest.java`
  - `grace-platform/src/main/java/com/grace/platform/metadata/interfaces/dto/response/VideoMetadataResponse.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P3-05, P1-07, P1-09
- **状态**: [ ]

---

### P3-07: 创建 Metadata MyBatis Mapper + JsonStringListTypeHandler

- **参考文档**: `docs/backend/04-context-metadata.md` §G.2（VideoMetadataMapper 接口 + 数据库列映射表 + tags_json 字段 JSON 序列化说明）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/persistence/VideoMetadataMapper.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/persistence/typehandler/JsonStringListTypeHandler.java`
  - `grace-platform/src/main/resources/mapper/metadata/VideoMetadataMapper.xml`
- **验证命令**: `mvn clean compile`
- **依赖**: P3-01, P1-05
- **状态**: [ ]
- **注意**: tags 字段在 DB 为 `tags_json` TEXT 存储 JSON 数组，通过 JsonStringListTypeHandler 在 ResultMap 中进行 List<String> ↔ JSON 转换

---

### P3-08: 创建 Metadata Repository 实现

- **参考文档**: `docs/backend/04-context-metadata.md` §G.2（VideoMetadataMapper 方法签名）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/persistence/VideoMetadataRepositoryImpl.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P3-07
- **状态**: [ ]

---

### P3-09: 创建 Metadata 属性测试

- **参考文档**: `docs/backend/10-testing-strategy.md` §B（Correctness Properties #4-#5：元数据字段约束不变量、元数据编辑往返）
- **产出文件**:
  - `grace-platform/src/test/java/com/grace/platform/metadata/MetadataPropertyTest.java`
- **验证命令**: `mvn test -Dtest="MetadataPropertyTest"`
- **依赖**: P3-01, P1-16
- **状态**: [ ]
- **测试要点**: Property #4 标题≤100/描述≤5000/标签5-15 不变量；Property #5 编辑后持久化再读取一致性；每个 Property ≥100 次迭代

---

### P3-10: 创建 Metadata 单元测试

- **参考文档**: `docs/backend/10-testing-strategy.md` §C（单元测试：LLM 降级处理、已确认元数据不可编辑）
- **产出文件**:
  - `grace-platform/src/test/java/com/grace/platform/metadata/MetadataUnitTest.java`
- **验证命令**: `mvn test -Dtest="MetadataUnitTest"`
- **依赖**: P3-01, P3-04, P1-16
- **状态**: [ ]
- **测试要点**: LLM 3次失败后抛 9001；confirmed=true 时调 update() 抛 2003；标签数<5或>15时 validate() 抛 2001
