# Lessons Learned

记录历次任务中遗漏的事项，避免后续重犯。新条目追加在最上方。

---

## L1 — 改 DB 表结构后忘记同步 `docs/backend/db-design.md`

**日期**：2026-05-05
**任务**：给 `video` 表新增 `ext_info` JSON 字段（V10 迁移）

**遗漏**
- 写了 Flyway migration、改了 Mapper / 聚合根 / DTO，但**没有同步更新** `docs/backend/db-design.md` 中的 `video` 表字段说明。
- 用户事后指出后才补救。

**根因**
- 任务规划时只盯着代码（migration → entity → mapper → dto），**把"文档"当成可选项**，未把它纳入 todo list。
- 代码改完即认为"完成"，没有跑一次"凡是改了 DDL，必动 db-design 文档"的反向检查。

**规则**（写入默认行为）
- **凡是新增 / 修改 / 删除 DB 表结构（`grace-platform/src/main/resources/db/migration/V*.sql`），必须同步更新 `docs/backend/db-design.md`。**
- 任务规划阶段，只要涉及 DDL，todo list 中**必须包含一项** "更新 db-design.md"。
- 同理：改 REST 接口要同步 `api.md`；改架构要同步 `architecture.md`；改需求要同步 `requirements.md`。

**自检清单（改前问自己一遍）**
1. 我改了哪些 DDL？→ 对应表在 db-design.md 哪一节？
2. 我加/删了字段、约束、索引？→ 文档里对应行是否同步？
3. 字段含义、JSON 结构约定是否在文档里写清楚（比如 ext_info 的 traceId/rpcId/createdTraceId）？
