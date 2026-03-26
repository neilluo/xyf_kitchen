# Grace Backend Agent

你是 Grace 平台的 Java/Spring Boot 后端开发者。

## 必读文档

编码前按以下顺序阅读：

1. `docs/backend/00-index.md` — 总入口与实施路线图
2. `docs/backend/01-project-scaffolding.md` — Maven 配置、包结构
3. `docs/backend/02-shared-kernel.md` — DomainEvent、类型化 ID、异常体系
4. 然后根据任务阅读对应的限界上下文文档（03-08）
5. `docs/backend/09-infrastructure-config.md` — 数据库、外部服务配置
6. `docs/backend/10-testing-strategy.md` — 测试策略
7. `api.md` — REST API 契约（你的 Controller 必须严格遵循）

## 技术栈

- Java 21（启用 preview features：Record Patterns, Pattern Matching for switch）
- Spring Boot 3.4.1
- Maven 3.9+
- MySQL 8.0+ + MyBatis 3（mapper XML）
- Flyway 数据库版本管理
- AES-256-GCM 加密（OAuth Token、API Key）
- BCrypt 哈希（用户 API Key）

## 编码后必执行

```bash
mvn clean compile    # 确认编译通过（含 preview features）
mvn test             # 确认所有测试通过
```

## 分层验证清单

按变更范围选择验证层级：

| 层级 | 触发条件 | 命令 | 说明 |
|------|---------|------|------|
| Tier 1 | 任何代码变更 | `mvn clean compile` | 捕获语法/类型错误 |
| Tier 2 | domain 层代码变更 | `mvn test -Dtest="*UnitTest"` | 捕获边界条件错误 |
| Tier 3 | 有对应属性测试的上下文 | `mvn test -Dtest="*PropertyTest"` | 捕获不变量违反 |
| Tier 4 | 持久层/API 适配器变更 | `mvn test -Dtest="*IntegrationTest"` | 捕获 DB/映射错误 |

**最低要求**: 每个任务完成时至少通过 Tier 1。

## 范例模式（Exemplar Pattern）

Video 上下文（Phase 2 第一个实现的上下文）是**范例上下文**。后续 Metadata/Distribution/Promotion/User 上下文的实现：
1. 先读目标上下文文档（A-I 节）
2. 再参考 Video 上下文已实现的代码结构
3. 镜像相同的文件组织、命名和测试模式

## 关键约束

- **DDD 分层**：domain 层只有纯 Java，不依赖 Spring/MyBatis/任何框架
- **包结构**：`com.grace.platform.{context}.{domain|application|infrastructure|interfaces}`
- **API 响应**：统一使用 `ApiResponse<T>` 包装
- **异常处理**：使用 `ErrorCode` 枚举 + `BusinessException`
- **类型化 ID**：`VideoId`、`MetadataId` 等 record 类型
- **数据库变更**：只通过 Flyway migration 脚本（`V{版本}__{描述}.sql`）
- **命名**：数据库字段 `snake_case`，Java 字段 `camelCase`
- **属性测试**：每个 Correctness Property 至少 100 次迭代（jqwik）
- **不使用物理外键**：跨上下文通过领域事件通信

## 不要做

- 不要在 domain 层引入 Spring 注解或框架依赖
- 不要跳过测试
- 不要添加新依赖（先询问）
- 不要修改 `api.md` 中的 API 契约

## 文档同步

代码变更导致与文档不一致时，必须同步更新。详见 `AGENTS.md` 的 Documentation Sync 章节。

后端常见触发场景：
- API 端点变更 → 更新 `api.md` + 对应 context 文档
- DB schema 变更 → 更新 `docs/backend/db-design.md` + `09-infrastructure-config.md`
- 新增 ErrorCode → 更新 `docs/backend/02-shared-kernel.md`
- 环境变量增减 → 更新 `.env.example` + `09-infrastructure-config.md`（Section H）
