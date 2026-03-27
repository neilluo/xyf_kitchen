# Phase 1: 项目脚手架 + 共享内核

> 参考实施路线图：`docs/backend/00-index.md` §6 Phase 1
> 产出：Maven 项目骨架 + 共享内核 + DB 迁移脚本 + 日志基础设施 + 测试基础设施

## 进度统计

- [x] 共 17 个任务，已完成 9/17

---

## 任务列表

### P1-01: 创建 Maven pom.xml

- **参考文档**: `docs/backend/01-project-scaffolding.md` §1.1（核心配置）、§1.2（依赖清单表，16 个依赖）、§1.3（Java 21 编译器 `--enable-preview`）
- **产出文件**:
  - `grace-platform/pom.xml`
- **验证命令**: `mvn clean compile` 退出码 0（此时无 Java 源码，仅验证 POM 合法性）
- **依赖**: 无
- **状态**: [x]

---

### P1-02: 创建项目目录结构 + Spring Boot 启动类

- **参考文档**: `docs/backend/01-project-scaffolding.md` §2.1（包结构树）、§4.1（启动类代码）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/GracePlatformApplication.java`
  - 创建所有限界上下文的空包目录（每个 context 下的 `domain/`、`application/`、`infrastructure/`、`interfaces/` 空 package-info 或占位）
- **验证命令**: `mvn clean compile`
- **依赖**: P1-01
- **状态**: [x]

---

### P1-03: 创建 application.yml + application-test.yml

- **参考文档**: `docs/backend/01-project-scaffolding.md` §3.1（完整 YAML）、§3.3（Resources 目录结构）；`docs/backend/10-testing-strategy.md` §E（测试 Profile）
- **产出文件**:
  - `grace-platform/src/main/resources/application.yml`
  - `grace-platform/src/test/resources/application-test.yml`（`spring.flyway.enabled=false`）
- **验证命令**: `mvn clean compile`
- **依赖**: P1-01
- **状态**: [x]

---

### P1-04: 创建 9 个类型化 ID record

- **参考文档**: `docs/backend/02-shared-kernel.md` §2.1（通用 ID 模式代码）、§2.2（全部 ID 清单：VideoId/MetadataId/PublishRecordId/OAuthTokenId/ChannelId/PromotionRecordId/UserProfileId/NotificationPreferenceId/ApiKeyId）
- **产出文件**（9 个文件）:
  - `grace-platform/src/main/java/com/grace/platform/shared/domain/id/VideoId.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/domain/id/MetadataId.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/domain/id/PublishRecordId.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/domain/id/OAuthTokenId.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/domain/id/ChannelId.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/domain/id/PromotionRecordId.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/domain/id/UserProfileId.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/domain/id/NotificationPreferenceId.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/domain/id/ApiKeyId.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-02
- **状态**: [x]

---

### P1-11: 创建加密服务（AES-256-GCM）

- **参考文档**: `docs/backend/02-shared-kernel.md` §6.1（EncryptionService 接口）、§6.2（AesGcmEncryptionService 实现骨架 — 需完成 encrypt/decrypt 方法体）
- **产出文件**（2 个文件）:
  - `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/encryption/EncryptionService.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/encryption/AesGcmEncryptionService.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-02, P1-08（EncryptionException 依赖）
- **状态**: [x]
- **注意**: §6.2 中 encrypt/decrypt 方法体为 `throw UnsupportedOperationException`，需实现完整的 AES-256-GCM 逻辑（IV 12字节 + Base64 编码）

---

### P1-12: 创建 API Key 哈希服务（BCrypt）

- **参考文档**: `docs/backend/02-shared-kernel.md` §6.3（ApiKeyHashService 接口 + BcryptApiKeyHashService 实现代码）
- **产出文件**（2 个文件）:
  - `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/encryption/ApiKeyHashService.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/encryption/BcryptApiKeyHashService.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-02
- **状态**: [x]

---

### P1-13: 创建日志基础设施

- **参考文档**: `docs/backend/log-design.md` §2.2（TraceIdFilter 代码）、§3（CachedBodyFilter 代码）、§4.1（RequestResponseLoggingInterceptor 代码）、§4.2（WebMvcConfig 代码）、§6（AsyncConfig + MdcTaskDecorator 代码）、§7（SlowSqlInterceptor 代码）；`docs/backend/02-shared-kernel.md` §8.1（组件清单与包路径）
- **产出文件**（7 个文件）:
  - `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/web/TraceIdFilter.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/web/CachedBodyFilter.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/web/RequestResponseLoggingInterceptor.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/web/WebMvcConfig.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/async/AsyncConfig.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/async/MdcTaskDecorator.java`
  - `grace-platform/src/main/java/com/grace/platform/shared/infrastructure/persistence/SlowSqlInterceptor.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-10（WebMvcConfig 可能引用 WebConfig 的配置）
- **状态**: [ ]

---

### P1-14: 创建 logback-spring.xml

- **参考文档**: `docs/backend/log-design.md` §5（完整 logback-spring.xml 配置，含 Console/File Appender、按天滚动、traceId Pattern）
- **产出文件**:
  - `grace-platform/src/main/resources/logback-spring.xml`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-03
- **状态**: [ ]

---

### P1-15: 创建 Flyway 迁移脚本 V1-V6

- **参考文档**: `docs/backend/db-design.md`（DDL 汇总）；各上下文文档 §I 节：`03-context-video.md` §I、`04-context-metadata.md` §I、`05-context-distribution.md` §I、`06-context-promotion.md` §I、`07-context-user-settings.md` §I；`docs/backend/01-project-scaffolding.md` §3.3（SQL 文件路径）
- **产出文件**（6 个文件）:
  - `grace-platform/src/main/resources/db/migration/V1__create_video_tables.sql`
  - `grace-platform/src/main/resources/db/migration/V2__create_metadata_tables.sql`
  - `grace-platform/src/main/resources/db/migration/V3__create_distribution_tables.sql`
  - `grace-platform/src/main/resources/db/migration/V4__create_promotion_tables.sql`
  - `grace-platform/src/main/resources/db/migration/V5__create_user_settings_tables.sql`
  - `grace-platform/src/main/resources/db/migration/V6__add_indexes.sql`
- **验证命令**: `mvn clean compile`（SQL 仅运行时校验，此处只检查文件存在且编译不报错）
- **依赖**: P1-03
- **状态**: [ ]

---

### P1-16: 创建测试基础设施

- **参考文档**: `docs/backend/10-testing-strategy.md` §E（AbstractIntegrationTest — Testcontainers MySQL 基类）、§F（TestFixtures 数据工厂）、§G（GraceArbitraries 自定义生成器）
- **产出文件**（3 个文件）:
  - `grace-platform/src/test/java/com/grace/platform/testutil/AbstractIntegrationTest.java`
  - `grace-platform/src/test/java/com/grace/platform/testutil/TestFixtures.java`
  - `grace-platform/src/test/java/com/grace/platform/testutil/GraceArbitraries.java`
- **验证命令**: `mvn clean compile` (test-compile)
- **依赖**: P1-04（TestFixtures 需要 ID 类型）
- **状态**: [x]

---

### P1-17: 创建 EncryptionService 单元测试

- **参考文档**: `docs/backend/10-testing-strategy.md` §B（测试目录结构 `shared/EncryptionServiceTest.java`）；`docs/backend/02-shared-kernel.md` §6.2（加密算法规格：IV 12字节, Tag 128bit, Base64 编码）
- **产出文件**:
  - `grace-platform/src/test/java/com/grace/platform/shared/EncryptionServiceTest.java`
- **验证命令**: `mvn test -Dtest="EncryptionServiceTest"` 通过
- **依赖**: P1-11, P1-16
- **状态**: [ ]
- **测试要点**: 加密后解密恢复原文、不同明文产生不同密文（随机 IV）、空值/空字符串处理
