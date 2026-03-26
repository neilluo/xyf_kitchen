# Grace Architecture

## 限界上下文

DDD 架构，5 个限界上下文 + 1 个跨上下文查询：

| 上下文 | 包路径 | 设计模式 | 领域事件 |
|--------|--------|----------|----------|
| Video | `com.grace.platform.video` | — | VideoUploadedEvent (发布) |
| Metadata | `com.grace.platform.metadata` | Adapter (LLM) | MetadataConfirmedEvent (发布) |
| Distribution | `com.grace.platform.distribution` | Strategy + Registry + Adapter + Template Method | VideoPublishedEvent (发布) |
| Promotion | `com.grace.platform.promotion` | Strategy + Registry + Adapter | — |
| User/Settings | `com.grace.platform.user` | — | — |
| Dashboard | `com.grace.platform.dashboard` | 跨上下文聚合查询 | — |

事件流：`VideoUploadedEvent` → `MetadataConfirmedEvent` → `VideoPublishedEvent`

## 后端包结构

每个上下文遵循 DDD 四层：

```
com.grace.platform.{context}/
├── domain/          # 实体、值对象、领域事件、仓储接口（纯 Java，不依赖框架）
├── application/     # 应用服务、DTO、事件监听
├── infrastructure/  # 仓储实现（MyBatis）、外部 API 适配器
└── interfaces/      # REST Controller
```

**关键约束**：domain 层只有纯 Java，不引入 Spring/MyBatis/任何框架依赖。

## 前端目录结构

```
src/
├── api/          # Axios 请求函数（按领域分文件：video.ts, metadata.ts...）
├── components/
│   ├── layout/   # AppLayout, Sidebar, Header
│   └── ui/       # 自建原子组件（Button, Card, StatusBadge, Table...）
├── hooks/        # React Query hooks（按领域分文件：useVideos.ts, useMetadata.ts...）
├── pages/        # 7 个页面组件（PascalCase + Page 后缀）
├── store/        # Zustand store（上传队列、Toast 通知）
├── types/        # TypeScript 类型定义（按领域分文件）
└── utils/        # 工具函数（格式化、状态映射、常量）
```

## 设计模式

### Strategy + Registry（Distribution & Promotion）

平台/渠道的可扩展接入，新增平台只需实现 Strategy 接口并注册到 Registry。

详见：
- `docs/backend/05-context-distribution.md`
- `docs/backend/06-context-promotion.md`

### Adapter（外部服务）

所有第三方 API 通过 Adapter 封装在 infrastructure 层：
- 阿里云通义千问 → LLM Adapter
- YouTube Data API → YouTube Adapter
- OpenCrawl → OpenCrawl Adapter

### Domain Event（跨上下文通信）

上下文间不直接依赖，通过 Spring ApplicationEvent 传递领域事件。

详见：`docs/backend/02-shared-kernel.md`（DomainEvent 基类定义）
