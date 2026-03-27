# Phase 7: Dashboard 查询 + 配置收尾 + 集成测试

> 参考实施路线图：`docs/backend/00-index.md` §6 Phase 5
> 产出：Dashboard 聚合查询、完整配置、集成测试、全量编译通过

## 进度统计

- [x] 共 8 个任务，已完成 8/8

---

## 任务列表

### P7-01: 创建 DashboardQueryService + DTO

- **参考文档**: `docs/backend/08-dashboard-query.md` §B1（DashboardQueryService 完整代码含 queryStats/queryRecentUploads/queryPublishDistribution/queryPromotionOverview/queryAnalytics 5 个私有方法）、§B2（各查询方法数据来源映射表）、§B3（resolveDateRange 代码）、§C（全部 DTO record 定义）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/dashboard/application/DashboardQueryService.java`
  - `grace-platform/src/main/java/com/grace/platform/dashboard/application/dto/DashboardOverviewResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/dashboard/application/dto/StatsDto.java`
  - `grace-platform/src/main/java/com/grace/platform/dashboard/application/dto/RecentUploadDto.java`
  - `grace-platform/src/main/java/com/grace/platform/dashboard/application/dto/PublishDistributionDto.java`
  - `grace-platform/src/main/java/com/grace/platform/dashboard/application/dto/PromotionOverviewDto.java`
  - `grace-platform/src/main/java/com/grace/platform/dashboard/application/dto/AnalyticsDto.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P2-04, P4-04, P5-04
- **状态**: [x]
- **注意**: @Transactional(readOnly=true)；依赖 VideoRepository/PublishRecordRepository/PromotionRecordRepository/PromotionChannelRepository 的只读查询

---

### P7-02: 创建 DashboardController

- **参考文档**: `docs/backend/08-dashboard-query.md` §D（DashboardController 完整代码，A1 端点）；`api.md` §A
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/dashboard/interfaces/rest/DashboardController.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P7-01, P1-07, P1-09
- **状态**: [x]

---

### P7-03: 扩展各 Mapper 添加 Dashboard 查询方法

- **参考文档**: `docs/backend/08-dashboard-query.md` §E（VideoMapper 扩展 4 方法 + PublishRecordMapper 扩展 2 方法 + PromotionRecordMapper 扩展 4 方法 + ChannelPromotionStats 投影接口）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/dashboard/application/dto/ChannelPromotionStats.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P2-08, P4-09, P5-10
- **状态**: [x]
- **注意**: 需要在已有的 VideoMapper/PublishRecordMapper/PromotionRecordMapper 接口中添加 count/countByStatus/countByStatusIn/findTop5 等方法，并在对应 XML 中添加 SQL。ChannelPromotionStats 为新投影接口

---

### P7-04: 完善 application.yml + 创建 .env.example

- **参考文档**: `docs/backend/09-infrastructure-config.md` §H（18 个环境变量清单）；`docs/backend/01-project-scaffolding.md` §3.1（完整 YAML 配置）
- **产出文件**:
  - `grace-platform/.env.example`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-03
- **状态**: [x]
- **注意**: .env.example 包含全部 18 个环境变量（MYSQL_HOST/PORT/DATABASE/USERNAME/PASSWORD、GRACE_ENCRYPTION_KEY、QWEN_API_KEY/MODEL/BASE_URL、YOUTUBE_CLIENT_ID/SECRET/REDIRECT_URI、OPENCRAWL_API_KEY/BASE_URL 等），值为占位符

---

### P7-05: 补全 TestFixtures + GraceArbitraries

- **参考文档**: `docs/backend/10-testing-strategy.md` §F（TestFixtures 全部工厂方法：createVideo/createUploadSession/createMetadata/createPublishRecord/createChannel/createPromotionRecord/createApiKey）、§G（GraceArbitraries 全部生成器）
- **产出文件**:
  - `grace-platform/src/test/java/com/grace/platform/testutil/TestFixtures.java`
  - `grace-platform/src/test/java/com/grace/platform/testutil/GraceArbitraries.java`
- **验证命令**: `mvn test-compile`
- **依赖**: P1-16, P2-01, P3-01, P4-01, P5-01, P6-01
- **状态**: [x]
- **注意**: 此任务是补全 P1-16 中创建的骨架，添加所有上下文的工厂方法和生成器

---

### P7-06: 创建 6 个集成测试

- **参考文档**: `docs/backend/10-testing-strategy.md` §D（集成测试场景表：VideoIntegrationTest/MetadataIntegrationTest/DistributionIntegrationTest/PromotionIntegrationTest/UserSettingsIntegrationTest/DashboardIntegrationTest）
- **产出文件**:
  - `grace-platform/src/test/java/com/grace/platform/video/VideoIntegrationTest.java`
  - `grace-platform/src/test/java/com/grace/platform/metadata/MetadataIntegrationTest.java`
  - `grace-platform/src/test/java/com/grace/platform/distribution/DistributionIntegrationTest.java`
  - `grace-platform/src/test/java/com/grace/platform/promotion/PromotionIntegrationTest.java`
  - `grace-platform/src/test/java/com/grace/platform/usersettings/UserSettingsIntegrationTest.java`
  - `grace-platform/src/test/java/com/grace/platform/dashboard/DashboardIntegrationTest.java`
- **验证命令**: `mvn test -Dtest="*IntegrationTest"`
- **依赖**: P7-05, P1-16
- **状态**: [x]
- **注意**: 继承 AbstractIntegrationTest（Testcontainers MySQL 8.0）；外部服务使用 @MockBean（LlmService/YouTubeApiAdapter/OpenCrawlAdapter）；需要 Docker 运行

---

### P7-07: 创建 LlmProperties + EncryptionProperties 配置类

- **参考文档**: `docs/backend/09-infrastructure-config.md`（Qwen LLM 配置项 + AES-256-GCM 加密配置项）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/config/LlmProperties.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/config/EncryptionProperties.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-02
- **状态**: [x]

---

### P7-08: 全量编译 + 全量测试通过

- **参考文档**: 无（验证性任务）
- **产出文件**: 无新文件，修复全部编译和测试错误
- **验证命令**: `mvn clean compile && mvn test`
- **依赖**: P7-01 至 P7-07
- **状态**: [x]
- **注意**: 此任务为后端收尾验证，确保全部代码编译通过且全部测试（属性测试+单元测试+集成测试）通过；如果 Docker 不可用则跳过集成测试：`mvn test -Dtest="*PropertyTest,*UnitTest"`
