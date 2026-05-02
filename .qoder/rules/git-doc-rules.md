---
trigger: always_on
---

# Git & Documentation Rules

## Conventional Commits
**Do**: 提交消息格式 `{type}({context}): {描述}`。
```
feat(video): implement chunked upload API
fix(metadata): validate tag count bounds
refactor(distribution): extract platform registry
test(promotion): add channel CRUD property test
docs(api): update metadata review endpoints
```
类型：`feat` | `fix` | `refactor` | `test` | `docs` | `chore`

分支命名：`{type}/{context}/{description}`，例如 `feat/video/chunked-upload`。

**Don't**: 使用模糊提交消息如 `update code`、`fix bug`、`wip`。
**Self-check**: 每个提交消息匹配正则 `^(feat|fix|refactor|test|docs|chore)\(.+\): .+$`。

## Documentation Sync — Code Is Source of Truth

当代码变更导致与文档不一致时，**必须同步更新对应文档**：

| 代码变更 | 必更文档 |
|---------|---------|
| API 端点签名变更 | `api.md` + 对应 context 文档 |
| 数据库 schema 变更 | `db-design.md` + `09-infrastructure-config.md` |
| 新增/移除依赖 | `AGENTS.md` Tech Stack 表 |
| 架构/设计模式变更 | `design.md` + 对应 context 文档 |
| 前端组件约定变更 | `docs/frontend/01-tech-stack-and-conventions.md` 或 `02-design-system.md` |
| 环境变量增减 | `.env.example` + `09-infrastructure-config.md` |
| 新增 ErrorCode | `02-shared-kernel.md` + `design.md` |
| 测试策略变更 | `10-testing-strategy.md` |

变更注释格式：`<!-- SYNC: {日期} | 原因: {简述} | 变更: {具体改动} -->`

**原则**: 只改受影响部分，保持原有结构，宁可更新也不要遗漏。

**Self-check**: 每次代码变更后检查以上表格中是否有受影响的文档，如有则同步更新。

## Progress & Learnings — Append-Only

**Do**: 任务完成后在 `.ai/progress.md` 追加一行记录，在 `.ai/learnings.md` 追加经验（非显而易见的问题）。
**Don't**: 编辑或删除已有的 progress/learnings 条目。
**Self-check**: 每次提交前检查 progress.md 和 learnings.md 已有内容未被修改。

## Common Pitfalls — Avoid These

1. **domain 层引 Spring 注解** — `domain/` 必须是纯 Java
2. **组件内直接调 axios** — 必须走 `src/api/` → `src/hooks/` → 组件
3. **使用第三方 UI 库** — 禁止 Ant Design / MUI / shadcn
4. **忘记 ApiResponse 包装** — 所有 REST 端点必须返回 `ApiResponse<T>`
5. **直接写 DDL 而非 Flyway** — 只通过 `V{N}__{description}.sql`
6. **跨上下文直接 import** — 通过领域事件通信

**Self-check**: 编码完成后对照以上 6 条逐一确认。
