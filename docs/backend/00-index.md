# Grace 后端技术文档索引

> 本文档集为 Grace 视频分发与推广平台的服务端实现指南，供 AI 编码代理按序阅读并完成 Java 后端代码编写。

## 1. 项目简介

Grace 是一个面向美食博主的视频分发与推广平台。博主上传本地视频后，由 AI（阿里云通义千问）自动生成元数据（标题/描述/标签），经用户审核确认后通过通用分发接口发布到视频平台（MVP 阶段聚焦 YouTube），并集成 OpenCrawl 在社交媒体和论坛自动执行推广。系统采用 DDD 架构，通过 Strategy + Registry 模式实现平台和渠道的可扩展接入。

## 2. 技术栈汇总

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 3.x |
| 构建工具 | Maven | 3.9+ |
| 架构风格 | DDD（领域驱动设计） | — |
| 数据库 | MySQL | 8.0+ |
| 持久层 | MyBatis 3 + MyBatis-Spring-Boot-Starter | — |
| LLM 服务 | 阿里云通义千问 API | — |
| 视频分发 | YouTube Data API v3 | — |
| 推广执行 | OpenCrawl Agentic API | — |
| API Key 加密 | AES-256-GCM | — |
| 用户 Key 哈希 | BCrypt | — |
| 认证 | OAuth 2.0（YouTube） | — |
| 单元测试 | JUnit 5 | — |
| 属性测试 | jqwik | — |
| Mock | Mockito | — |
| 集成测试 | Testcontainers（MySQL） | — |

## 3. 文档导航

| 编号 | 文件 | 说明 | 层级 |
|------|------|------|------|
| 00 | [00-index.md](./00-index.md) | 文档索引与实施路线图 | L1 索引 |
| 01 | [01-project-scaffolding.md](./01-project-scaffolding.md) | Maven 配置、包结构、application.yml | L2 基础设施 |
| 02 | [02-shared-kernel.md](./02-shared-kernel.md) | DomainEvent、类型化 ID、ApiResponse、异常体系、加密 | L2 基础设施 |
| 03 | [03-context-video.md](./03-context-video.md) | 视频上下文：分片上传、文件管理 | L3 限界上下文 |
| 04 | [04-context-metadata.md](./04-context-metadata.md) | 元数据上下文：LLM 生成、用户审核 | L3 限界上下文 |
| 05 | [05-context-distribution.md](./05-context-distribution.md) | 分发上下文：Strategy+Registry、YouTube、OAuth | L3 限界上下文 |
| 06 | [06-context-promotion.md](./06-context-promotion.md) | 推广上下文：Strategy+Registry、OpenCrawl、渠道管理 | L3 限界上下文 |
| 07 | [07-context-user-settings.md](./07-context-user-settings.md) | 用户设置上下文：资料、通知、API Key | L3 限界上下文 |
| 08 | [08-dashboard-query.md](./08-dashboard-query.md) | 跨上下文仪表盘聚合查询 | L3 跨上下文 |
| 09 | [09-infrastructure-config.md](./09-infrastructure-config.md) | 数据库、外部服务、安全配置 | L2 基础设施 |
| 10 | [10-testing-strategy.md](./10-testing-strategy.md) | 属性测试、单元测试、集成测试 | L2 基础设施 |
| DB | [db-design.md](./db-design.md) | 数据库设计规范、RDS 连接、Flyway 版本管理 | L2 基础设施 |
| LOG | [log-design.md](./log-design.md) | 日志规范、Trace ID 链路追踪、按天滚动 | L2 基础设施 |

## 4. 限界上下文 — API — 设计模式映射表

| 限界上下文 | 包路径 | API 端点编号 | 设计模式 | 领域事件 |
|-----------|--------|-------------|---------|---------|
| Video | `com.grace.platform.video` | B1-B6 | — | VideoUploadedEvent (发布) |
| Metadata | `com.grace.platform.metadata` | C1-C5 | Adapter (LLM) | MetadataConfirmedEvent (发布) |
| Distribution | `com.grace.platform.distribution` | D1-D6 | Strategy + Registry + Adapter (YouTube) + Template Method | VideoPublishedEvent (发布) |
| Promotion | `com.grace.platform.promotion` | E1-E5, F1-F5 | Strategy + Registry + Adapter (OpenCrawl) | — |
| User/Settings | `com.grace.platform.user` | G1-G10 | — | — |
| Dashboard | `com.grace.platform.dashboard` | A1 | 跨上下文聚合查询 | — |

## 5. 文档依赖关系

```
00-index.md (总入口)
│
├── 01-project-scaffolding.md (首先阅读 — 项目骨架)
│
├── 02-shared-kernel.md (所有上下文的基础依赖)
│   └── DomainEvent、类型化ID、ApiResponse、异常体系、加密服务
│
├── 03-context-video.md
│   └── 发布 VideoUploadedEvent → 04 订阅
│
├── 04-context-metadata.md
│   ├── 订阅 VideoUploadedEvent ← 03
│   └── 发布 MetadataConfirmedEvent → 05
│
├── 05-context-distribution.md
│   ├── 订阅 MetadataConfirmedEvent ← 04
│   └── 发布 VideoPublishedEvent → 06
│
├── 06-context-promotion.md
│   └── 订阅 VideoPublishedEvent ← 05（可选自动触发）
│
├── 07-context-user-settings.md
│   └── 读取 05 的 OAuthToken（已连接账户）
│
├── 08-dashboard-query.md
│   └── 读取 03(Video) + 05(Distribution) + 06(Promotion) 的 Repository
│
├── 09-infrastructure-config.md (全局配置汇总)
│
├── 10-testing-strategy.md (质量保障)
│
├── db-design.md (数据库设计规范)
│   ├── 引用 09 的 DDL 汇总
│   ├── RDS 连接、HikariCP、Flyway 版本管理
│   └── 被 01(依赖清单) 和 09(配置) 引用
│
└── log-design.md (日志设计规范)
    ├── TraceIdFilter → 依赖 02 shared-kernel 包结构
    ├── RequestResponseLoggingInterceptor → 所有 Controller(03-07) 的请求日志
    ├── logback-spring.xml → 被 01(文件清单) 引用
    └── SlowSqlInterceptor → 与 db-design.md 关联
```

## 6. 实施路线图

| 阶段 | 文档 | 产出 | 前置依赖 |
|------|------|------|---------|
| Phase 1 | 01 + 02 + db-design + log-design | Maven 项目骨架 + 共享内核 + DB/日志规范 | 无 |
| Phase 2 | 03 + 04 | 视频上传 + AI 元数据生成 | Phase 1 |
| Phase 3 | 05 | YouTube 视频分发 + OAuth | Phase 2 |
| Phase 4 | 06 + 07 | 推广执行 + 用户设置 | Phase 3 |
| Phase 5 | 08 + 09 + 10 | 仪表盘 + 配置收尾 + 测试 | Phase 4 |

## 7. 后端与前端文档交叉引用

| 后端文档 | 前端文档 | api.md 章节 |
|---------|---------|------------|
| 03-context-video.md | 05-page-video-management.md, 06-page-video-upload.md | B1-B6 |
| 04-context-metadata.md | 07-page-metadata-review.md | C1-C5 |
| 05-context-distribution.md | 08-page-distribution-promotion.md | D1-D6 |
| 06-context-promotion.md | 08-page-distribution-promotion.md, 09-page-promotion-history.md | E1-E5, F1-F5 |
| 07-context-user-settings.md | 10-page-settings.md | G1-G10 |
| 08-dashboard-query.md | 04-page-dashboard.md | A1 |

## 8. 限界上下文文档标准结构（03-07 通用）

每个限界上下文文档遵循 **A-I 九部分固定结构**：

| 部分 | 标题 | 内容类型 |
|------|------|----------|
| A | 上下文概览 | 职责说明、Mermaid 上下文图、包结构清单 |
| B | 领域模型 | 聚合根/实体/值对象/枚举字段表、状态机图、校验规则 |
| C | 领域服务与领域事件 | 接口签名、事件定义、设计模式详解 |
| D | 仓储接口 | Repository 方法签名表 |
| E | 应用层服务 | ApplicationService 方法签名表、Mermaid 时序图 |
| F | REST 控制器 | 端点映射表、Request/Response DTO 字段表 |
| G | 基础设施层实现 | Adapter 接口、MyBatis Mapper 映射、设计模式实现 |
| H | 错误处理 | 上下文专属错误码表、重试策略 |
| I | 数据库 Schema | DDL、索引、ER 片段 |

## 9. 设计原则

1. **高扩展性** — 新增视频平台只需实现 `VideoDistributor` 接口并加 `@Component`；新增推广方式只需实现 `PromotionExecutor` 接口。无需修改 Controller、ApplicationService、Registry。
2. **强内聚** — 每个限界上下文内部完整封装领域逻辑。上下文间仅通过领域事件通信，不直接调用彼此的应用服务。
3. **重设计模式** — Strategy（分发/推广的平台/渠道抽象）、Registry（动态路由）、Adapter（外部 API 封装）、Domain Event（上下文通信）、Template Method（分发流程模板化）、Factory（ApiResponse 静态工厂）、Value Object（类型化 ID）。
4. **DDD 分层约束** — 依赖方向只能：`interfaces → application → domain ← infrastructure`。domain 层不依赖任何其他层。
5. **接受代码复杂不接受逻辑分散** — 宁可每个上下文内部代码量多、类文件多，也要保证每个类只承担单一职责，逻辑不跨层不跨上下文分散。
