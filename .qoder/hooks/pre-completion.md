# Pre-Completion Checklist

Before declaring any task complete, verify every item below.

## Code Quality
- [ ] 后端代码编译通过（`cd grace-platform && mvn clean compile -q`）
- [ ] 前端无 ESLint 报错（`cd grace-frontend && npm run lint`）
- [ ] 前端无 TypeScript 类型错误（`cd grace-frontend && npx tsc --noEmit`）

## Tests
- [ ] 所有现有测试仍通过（根据变更类型选跑，见 `.qoder/rules/testing-rules.md` 测试矩阵）
- [ ] 新功能有对应测试（如适用）

## Cleanup
- [ ] 无死代码（未使用的 import、函数、变量、类型）
- [ ] 无被注释掉的旧代码
- [ ] 无新增 TODO/FIXME 未追踪

## Architecture
- [ ] DDD 分层约束未被违反（domain 层纯 Java，无 Spring/MyBatis 依赖）
- [ ] 所有 REST 端点返回 `ApiResponse<T>` 包装
- [ ] 数据库变更只通过 Flyway migration 脚本
- [ ] 跨上下文通过领域事件通信，无直接 import
- [ ] 前端无第三方 UI 库（Ant Design / MUI / shadcn）
- [ ] 前端组件通过 hooks 消费数据，无组件内直接用 axios

## Git
- [ ] Conventional Commits 格式（`type(context): description`）
- [ ] `.env` 文件未被提交

## Documentation
- [ ] 代码变更导致文档不一致时已同步更新（见 AGENTS.md → Documentation Sync）
- [ ] `.ai/progress.md` 已追加完成记录
- [ ] `.ai/learnings.md` 已追加经验（如遇非显而易见问题）
- [ ] 当前 phase 任务文件中的任务状态已标记为 `[x]`
