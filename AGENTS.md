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

## Boundaries

### Always Do

- 编码前阅读对应的 `docs/` 文档
- 遵循 `api.md` 中定义的请求/响应契约
- 为新功能编写测试（后端：属性测试 + 单元测试；前端：组件测试）
- REST 响应使用 `ApiResponse<T>` 包装
- 数据库变更通过 Flyway migration
- 编码完成后运行 lint 和类型检查
- **代码变更与文档不一致时，按 Documentation Sync 规则同步更新文档**

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
