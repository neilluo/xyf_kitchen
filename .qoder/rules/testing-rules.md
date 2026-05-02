---
trigger: "**/*Test*"
---

# Testing Rules

## Test Matrix — Run the Right Test for the Change

| 变更内容 | 测试类型 | 命令 | 文件命名 |
|---------|---------|------|---------|
| 领域逻辑（实体/值对象） | 属性测试（jqwik, ≥100 次） | `mvn test -Dtest="*PropertyTest"` | `{Context}PropertyTest.java` |
| 领域逻辑（边界条件） | 单元测试（JUnit 5 + AssertJ） | `mvn test -Dtest="*UnitTest"` | `{Context}UnitTest.java` |
| 仓储/API 集成 | 集成测试（Testcontainers） | `mvn test -Dtest="*IntegrationTest"` | `{Context}IntegrationTest.java` |
| 外部服务适配器 | Mock 测试 | `mvn test` | 集成测试内 |
| 前端组件 | Lint + 类型检查 | `npm run lint && npx tsc --noEmit` | — |

**Self-check**: 编码后根据变更类型选择对应的测试命令执行。

## jqwik — Domain Invariants
**Do**: 每个 Correctness Property 至少 100 次迭代，使用 `Arbitraries` 工厂类生成数据。
```java
@Property
boolean videoIdCannotBeNull(@ForAll @NonNull String value) {
    VideoId id = new VideoId(value);
    return id.value().equals(value);
}
```
**Don't**: 在属性测试中使用 Spring 上下文（`@SpringBootTest`）。纯领域属性测试不启动容器。
**Self-check**: 属性测试类不继承 `AbstractIntegrationTest`。

## Testcontainers — Integration Tests
**Do**: 继承 `AbstractIntegrationTest`（提供 Testcontainers MySQL 8.0）。需要 Repository 的属性测试必须继承它。
**Don't**: 在集成测试中手动启动 Spring 上下文或使用 `withInitScript`（与 Flyway 冲突）。
**Self-check**: 集成测试类继承 `AbstractIntegrationTest`，`application-test.yml` 中 `spring.flyway.enabled=false`。

## Test Data — Factory Pattern
**Do**: 使用 `TestFixtures` 工厂类创建测试数据，保证一致性。
```java
Video video = TestFixtures.video().withStatus(VideoStatus.PUBLISHED).build();
```
**Don't**: 在测试中硬编码测试数据（魔法字符串、魔法数字）。
**Self-check**: 测试代码中没有 `new Video("test", "http://...", "draft")` 这种硬编码构造。

## Test Workflow
新测试会话加载顺序：`AGENTS.md` → `.ai/progress.md`(最后20行) → `.ai/learnings.md` → `.ai/test-progress.md` → `.ai/test-learnings.md` → `git log --oneline -10`

测试后必更新：
- `.ai/test-progress.md` — 追加测试记录
- `.ai/test-learnings.md` — 追加经验（如有新问题）
- `test-results/` — 提交测试报告（格式 `YYYY-MM-DD-[主题]-REPORT.md`）

**Self-check**: 每次测试会话结束后确认三个文件都已更新。
