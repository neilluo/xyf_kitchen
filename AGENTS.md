# Grace Platform

面向美食博主的视频分发与推广平台。上传视频 → AI 生成元数据 → 用户审核 → 发布到 YouTube → 社交媒体自动推广。

## Agent 角色

你是 Grace 平台的全栈开发者。这是一个文档先行（documentation-first）项目——所有架构决策、API 契约、数据库设计均已在 `docs/` 中预先定义。你的职责是严格按照这些规格说明实现代码。

- 后端/前端的领域专属规则，参见 `.qoder/agents/` 下的专属 agent 配置
- 当前状态：项目尚无源代码，需从零搭建

## Architecture

DDD 架构，5 个限界上下文（Video, Metadata, Distribution, Promotion, User/Settings）+ 1 个跨上下文查询（Dashboard）。每个上下文遵循 `domain/` → `application/` → `infrastructure/` → `interfaces/` 四层结构。

包结构、前端目录、设计模式与事件流详见 [`architecture.md`](./architecture.md)。

## Tech Stack

### Backend

| 技术 | 版本 |
|------|------|
| Java | 21（启用 preview features） |
| Spring Boot | 3.4.1 |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| MyBatis 3 | mybatis-spring-boot-starter |
| Flyway | 数据库版本管理 |
| jqwik | 属性测试 |
| JUnit 5 + Mockito + AssertJ | 单元测试 |
| Testcontainers | 集成测试（MySQL 容器） |

### Frontend

| 技术 | 版本 |
|------|------|
| React + TypeScript | React 18, TS 5.x |
| Vite | 5.x |
| Tailwind CSS | v3（自定义 token） |
| React Router | v6 |
| TanStack Query | v5 |
| Zustand | v4 |
| Axios | 1.x |
| Recharts | 2.x |

### External Services

- 阿里云通义千问 API（LLM 元数据生成）
- YouTube Data API v3（视频发布）
- OpenCrawl Agentic API（社交推广执行）

## Commands

### Backend

```bash
mvn clean compile                     # 编译（含 Java 21 preview）
mvn test                              # 运行所有测试
mvn spring-boot:run                   # 启动后端（端口 8080）
mvn flyway:migrate                    # 执行数据库迁移
mvn test -pl . -Dtest="*PropertyTest" # 只跑属性测试
```

### Frontend

```bash
npm install                           # 安装依赖
npm run dev                           # 启动开发服务器（端口 3000）
npm run build                         # 构建生产版本
npm run lint                          # ESLint 检查
npx tsc --noEmit                      # TypeScript 类型检查
```

## Agent 工作流协议

每次编码会话遵循以下循环：

1. **加载上下文** — 读取当前 Phase 任务文件（`.ai/tasks/phase-N-*.md`），找到下一个未完成任务（状态为 `[ ]`）
2. **阅读文档** — 只读取该任务"参考文档"字段指定的文档章节，不加载无关文档
3. **实现** — 编写该任务"产出文件"列出的代码文件
4. **验证** — 执行该任务"验证命令"；若失败则修复并重试（最多 3 次）
5. **提交** — 验证通过后 `git add` + `git commit`（Conventional Commits 格式）
6. **记录** — 在 `.ai/progress.md` 追加一行完成记录
7. **学习** — 如遇非显而易见的问题，在 `.ai/learnings.md` 追加一条经验
8. **更新状态** — 将任务状态改为 `[x]`
9. **下一个** — 重复步骤 1

### 会话恢复

新会话开始时：
1. 读取 `.ai/progress.md` 最后 20 行，了解上次进度
2. 读取 `.ai/learnings.md`，加载积累的经验
3. 读取当前 Phase 任务文件，从第一个 `[ ]` 任务继续

### 阻塞处理

如果一个任务连续 3 次验证失败：
1. 将状态标记为 `[!]`（阻塞）
2. 在 `.ai/progress.md` 记录失败原因
3. 跳过该任务，继续下一个无依赖冲突的任务
4. 在 `.ai/learnings.md` 记录导致阻塞的问题

### 任务文件

- 原子任务分解文件位于 `.ai/tasks/` 目录
- 进度日志：`.ai/progress.md`
- 学习笔记（Running Notebook）：`.ai/learnings.md` — agent 在开发中积累的经验，append-only

## Testing Quick Reference

| 变更内容 | 测试类型 | 命令 | 文件命名 |
|---------|---------|------|---------|
| 领域逻辑（实体/值对象） | 属性测试（jqwik, ≥100 次） | `mvn test -Dtest="*PropertyTest"` | `{Context}PropertyTest.java` |
| 领域逻辑（边界条件） | 单元测试（JUnit 5 + AssertJ） | `mvn test -Dtest="*UnitTest"` | `{Context}UnitTest.java` |
| 仓储/API 集成 | 集成测试（Testcontainers） | `mvn test -Dtest="*IntegrationTest"` | `{Context}IntegrationTest.java` |
| 外部服务适配器 | Mock 测试（Mockito @MockBean） | `mvn test` | 集成测试内 |
| 前端组件 | Lint + 类型检查 | `npm run lint && npx tsc --noEmit` | — |

- 测试数据：使用 `TestFixtures` 工厂类
- 自定义生成器：`Arbitraries`（`testutil/Arbitraries.java`）
- 集成测试基类：`AbstractIntegrationTest`（Testcontainers MySQL 8.0）
- 共 12 个 Correctness Properties，详见 `docs/backend/10-testing-strategy.md`

## Git Workflow

### 分支命名

`{type}/{context}/{description}`

示例：`feat/video/chunked-upload`、`fix/metadata/tag-validation`、`refactor/shared/error-codes`

### 提交信息

Conventional Commits 格式：`{type}({context}): {描述}`

```
feat(video): implement chunked upload API
fix(metadata): validate tag count bounds
refactor(distribution): extract platform registry
test(promotion): add channel CRUD property test
docs(api): update metadata review endpoints
chore(deps): upgrade Spring Boot to 3.4.2
```

类型：`feat` | `fix` | `refactor` | `test` | `docs` | `chore`

### PR 约定

- 尽量一个限界上下文一个 PR
- 跨上下文变更需明确说明理由
- 提交前运行对应的编译/lint/测试命令

## Documentation Map

实施前**必须**阅读对应文档：

| 任务 | 入口文档 |
|------|----------|
| 后端实施 | `docs/backend/00-index.md`（按编号顺序读） |
| 前端实施 | `docs/frontend/00-index.md`（按编号顺序读） |
| API 契约 | `api.md`（30+ 端点定义） |
| 数据库设计 | `docs/backend/db-design.md` |
| 测试策略 | `docs/backend/10-testing-strategy.md` |
| 基础设施配置 | `docs/backend/09-infrastructure-config.md` |
| 日志规范 | `docs/backend/log-design.md` |
| 设计系统 | `docs/frontend/02-design-system.md` |
| 需求与验收标准 | `requirements.md` |
| 技术架构 | `design.md` |
| UI 原型 | `ui/stitch_grace_video_management/` 下各页面 HTML + 截图 |

## 上下文加载策略

为避免上下文窗口溢出，agent 按以下规则加载文档：

### 每次会话必加载
- `AGENTS.md`（主指令）
- `.ai/learnings.md`（积累的经验）
- `.ai/tasks/{当前phase}.md`（当前任务列表）
- `.ai/progress.md` 最后 20 行（最近进度）

### 按任务加载
- 该任务"参考文档"字段指定的文档章节
- `api.md` 中对应端点章节（如 Video 任务只读 §B）
- `02-shared-kernel.md`（仅当任务涉及共享基础设施时）

### 不要预加载
- 其他限界上下文的文档
- 做后端时不加载前端文档（反之亦然）
- `design.md`、`requirements.md`（仅在需要理解意图时按需查阅）
- UI 原型 HTML（仅在实现对应页面时加载）

## Coding Standards

### Backend (Java)

```java
// 好：类型化 ID
public record VideoId(String value) {}

// 好：ApiResponse 包装
return ApiResponse.success(videoDto);

// 好：异常体系
throw new BusinessException(ErrorCode.VIDEO_NOT_FOUND, videoId);

// 好：领域事件
eventPublisher.publish(new VideoUploadedEvent(videoId));
```

- Java 21 preview features：Record Patterns, Pattern Matching for switch
- DDD 分层约束：domain 层只有纯 Java，不依赖 Spring/MyBatis
- 数据库字段 `snake_case`，Java 字段 `camelCase`，MyBatis 自动映射
- API 统一使用 `ApiResponse<T>` 包装，错误使用 `ErrorCode` 枚举
- 数据库变更只通过 Flyway migration 脚本（`V{版本}__{描述}.sql`）
- OAuth Token / API Key 使用 AES-256-GCM 加密存储
- 用户 API Key 使用 BCrypt 单向哈希

### Frontend (TypeScript/React)

```typescript
// 好：Props 接口命名
interface StatusBadgeProps {
  status: VideoStatus;
  size?: 'sm' | 'md';
}

// 好：通过 hooks 消费数据
const { data, isLoading } = useVideos(filters);

// 好：事件处理命名
const handleSubmit = useCallback(() => { ... }, []);
```

- `strict: true`，优先 `interface`，联合类型用 `type`
- 全部函数组件 + Hooks，禁止 class 组件
- 全部 Tailwind CSS，不写自定义 CSS
- API 调用链路：`src/api/` → `src/hooks/` → 组件，禁止组件内直接用 axios
- 文件命名：组件 `PascalCase.tsx`，hooks `useCamelCase.ts`，类型 `camelCase.ts`

## Documentation Sync

代码是唯一的事实来源（source of truth）。当代码变更导致与文档不一致时，**必须同步更新对应文档**。

### 触发规则

| 代码变更类型 | 必须更新的文档 |
|-------------|---------------|
| API 端点签名变更（路径/参数/响应） | `api.md`，对应的 `docs/backend/0X-context-*.md`，对应的 `docs/frontend/0X-page-*.md` |
| 数据库 schema 变更 | `docs/backend/db-design.md`，`docs/backend/09-infrastructure-config.md`（DDL 部分） |
| 新增/移除依赖 | `AGENTS.md`（Tech Stack 表），对应的 scaffolding 文档 |
| 架构/设计模式变更 | `design.md`，对应的 context 文档 |
| 前端组件约定变更 | `docs/frontend/01-tech-stack-and-conventions.md` 或 `02-design-system.md` |
| 环境变量增减 | `.env.example`，`docs/backend/09-infrastructure-config.md`（Section H） |
| 新增 ErrorCode | `docs/backend/02-shared-kernel.md`，`design.md`（错误码部分） |
| 测试策略变更 | `docs/backend/10-testing-strategy.md` |

### 更新格式

在被修改文档的变更位置附近，添加变更注释：

```markdown
<!-- SYNC: 2026-03-26 | 原因: 修复上传接口增加 format 校验 | 变更: 新增 INVALID_FORMAT 错误码 -->
```

格式：`<!-- SYNC: {日期} | 原因: {简述} | 变更: {具体改动} -->`

### 原则

- **只改受影响的部分**，不重写整个文档
- **保持文档原有结构和风格**，不改变章节编号
- 如果不确定某处是否需要更新，**宁可更新也不要遗漏**
- 变更涉及多个文档时，在同一次任务中一并更新，不要拆分到后续任务

## Common Pitfalls

1. **在 domain 层引入 Spring 注解** — `domain/` 包必须是纯 Java。`@Service`、`@Autowired` 等放在 `infrastructure/` 或 `application/` 层
2. **组件内直接调用 axios** — 必须走 `src/api/` → `src/hooks/` → 组件的链路
3. **使用第三方 UI 库** — 禁止 Ant Design / MUI / shadcn 等，所有 UI 组件自建于 `src/components/ui/`
4. **忘记 ApiResponse 包装** — 所有 REST 端点必须返回 `ApiResponse<T>`
5. **直接写 DDL 而非 Flyway** — 数据库变更必须通过 `V{N}__{description}.sql` 迁移脚本
6. **跨上下文直接 import** — 限界上下文间通过领域事件通信，不可 `import com.grace.platform.video.*` 从 metadata 包内

## Troubleshooting

### Java 21 Preview Features
`--enable-preview` 必须同时配置在 `maven-compiler-plugin` 和 `maven-surefire-plugin` 中。缺少 surefire 配置会导致测试编译失败。需要 `<argLine>--enable-preview</argLine>`。

### MyBatis TypeHandler 注册
若类型化 ID 无法解析，检查 `application.yml` 中 `mybatis.type-handlers-package` 是否匹配实际包路径。每个 TypeHandler 的 `@MappedTypes` 需引用正确的 ID 类。

### jqwik 与 Spring Boot Test
jqwik 属性测试默认不启动 Spring 上下文。需要 Repository 的属性测试必须继承 `AbstractIntegrationTest`。纯领域属性测试不应使用 Spring 上下文。

### Testcontainers 依赖 Docker
集成测试需要 Docker 运行。若 Docker 不可用，跳过集成测试：`mvn test -Dtest="*PropertyTest,*UnitTest"`

### Flyway 与 Testcontainers 冲突
`AbstractIntegrationTest` 使用 `withInitScript` 可能与 Flyway 自动迁移冲突。在 `application-test.yml` 中设置 `spring.flyway.enabled=false`。

### 前端 CORS
Vite 开发服务器端口 3000 需要后端 CORS 放行。检查 `WebConfig` 的 allowedOrigins 是否包含 `http://localhost:3000`，或使用 Vite 的 proxy 配置代理 `/api` 请求。

### Duration 序列化
`java.time.Duration` JSON 序列化为 ISO 8601（`PT12M34S`），但数据库存储为 `BIGINT` 秒数。在 Mapper XML 中用 `duration_seconds` 列映射到 `long`，在领域实体构造器中转换。

## 测试工作流

### 测试启动流程

新测试会话开始时：

1. **加载上下文**
   - 读取 `AGENTS.md` - 项目指令、技术栈、测试策略
   - 读取 `.ai/progress.md` (最后 20 行) - 最近开发进度
   - 读取 `.ai/learnings.md` - 开发经验
   - 读取 `.ai/test-progress.md` - 测试进度（如有）
   - 读取 `.ai/test-learnings.md` - 测试经验（如有）
   - 检查 `git log --oneline -10` - 最新变更

2. **了解项目情况**
   - 技术栈（Java 21, Spring Boot 3.4.1, React 18）
   - 核心功能（视频上传、多模态元数据、YouTube 发布）
   - 测试策略（属性测试、单元测试、集成测试、E2E）
   - 常见陷阱（Java 21 preview, Testcontainers, Flyway）

3. **制定测试计划**
   - 根据 AGENTS.md 选择测试命令
   - 后端：`mvn test -Dtest="*PropertyTest,*UnitTest,*IntegrationTest"`
   - 前端：`npm run lint && npx tsc --noEmit`
   - E2E: `npx playwright test`

4. **执行测试**
   - 运行测试
   - 收集结果
   - 生成报告（使用 `test-results/TEMPLATE.md`）

5. **更新状态**
   - 更新 `.ai/test-progress.md` - 追加测试记录
   - 更新 `.ai/test-learnings.md` - 追加测试经验（如有新问题）
   - 提交测试报告到 `test-results/`

### 测试记录约定

#### 进度记录 (.ai/test-progress.md)

```markdown
## YYYY-MM-DD - [测试主题]

### 测试范围
- [ ] 测试项 1
- [ ] 测试项 2

### 测试结果
| 测试项 | 状态 | 说明 |
|--------|------|------|
| ... | ✅/❌ | ... |

### 发现的问题
1. ❌ [问题描述]

### 下一步
- [ ] ...
```

#### 经验记录 (.ai/test-learnings.md)

```markdown
## YYYY-MM-DD - [主题]

**问题:** [简述]

**发现:**
- ...

**解决方案:**
...

**教训:** [一句话总结]
```

### 测试报告模板

使用 `test-results/TEMPLATE.md` 作为标准模板。

**测试报告文件名格式:** `YYYY-MM-DD-[测试主题]-REPORT.md`

示例:
- `2026-03-29-MULTIMODAL-TEST-REPORT.md`
- `2026-03-29-10S-SCHEME-TEST-REPORT.md`
- `2026-03-29-FULL-REGRESSION-REPORT.md`

### 测试策略参考

详见 `docs/backend/10-testing-strategy.md` - 12 个 Correctness Properties

| 变更内容 | 测试类型 | 命令 | 文件命名 |
|---------|---------|------|---------|
| 领域逻辑（实体/值对象） | 属性测试（jqwik, ≥100 次） | `mvn test -Dtest="*PropertyTest"` | `{Context}PropertyTest.java` |
| 领域逻辑（边界条件） | 单元测试（JUnit 5 + AssertJ） | `mvn test -Dtest="*UnitTest"` | `{Context}UnitTest.java` |
| 仓储/API 集成 | 集成测试（Testcontainers） | `mvn test -Dtest="*IntegrationTest"` | `{Context}IntegrationTest.java` |
| 外部服务适配器 | Mock 测试（Mockito @MockBean） | `mvn test` | 集成测试内 |
| 前端组件 | Lint + 类型检查 | `npm run lint && npx tsc --noEmit` | — |

---

## Boundaries

### Always Do

- 编码前阅读对应的 `docs/` 文档
- 遵循 `api.md` 中定义的请求/响应契约
- 为新功能编写测试（后端：属性测试 + 单元测试；前端：组件测试）
- REST 响应使用 `ApiResponse<T>` 包装
- 数据库变更通过 Flyway migration
- 编码完成后运行 lint 和类型检查
- **代码变更与文档不一致时，按 Documentation Sync 规则同步更新文档**
- **测试前主动加载项目 AGENTS.md 了解项目情况**
- **测试后更新 .ai/test-progress.md 和 .ai/test-learnings.md**

### Ask First

- 添加新的 Maven/npm 依赖
- 修改 `api.md` 中的 API 契约
- 修改数据库 schema（需同步更新 Flyway 脚本）
- 引入新的设计模式或架构变更
- 跨限界上下文的直接依赖

### Never Do

- 提交 `.env` 文件或任何密钥/凭据
- 在 domain 层引入 Spring/MyBatis/框架依赖
- 使用第三方 UI 组件库（使用自建原子组件）
- 跳过测试直接提交
- 使用物理外键（跨上下文通过领域事件通信）
