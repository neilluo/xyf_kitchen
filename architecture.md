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
├── domain/                       # 实体、值对象、领域事件、仓储接口（纯 Java，不依赖框架）
├── application/                  # 应用服务、Command、应用层 DTO、事件监听
├── infrastructure/               # 仓储实现（MyBatis）、外部 API 适配器、配置
└── interfaces/
    └── rest/                     # REST Controller（@RestController）
        ├── XxxController.java
        └── dto/
            ├── request/          # 请求 DTO（带 @Valid 校验注解）
            └── response/         # 响应 DTO
```

**关键约束**：domain 层只有纯 Java，不引入 Spring/MyBatis/任何框架依赖。

> 为什么 Controller 在 `interfaces/rest/` 而不是 `interfaces/` 直下？
> 预留同层兄弟包的扩展位（未来可能加 `interfaces/mq/`、`interfaces/scheduler/`、`interfaces/grpc/`），所有"系统对外的入站适配器"统一放在 `interfaces/` 下按协议分包。

## 分层约定

### 1. 各层职责边界

| 层 | 包 | 职责 | 典型产物 |
|---|---|---|---|
| **interfaces** | `interfaces.rest` | 协议适配（HTTP）、参数绑定与校验、调用 ApplicationService、组装响应 DTO | Controller、Request/Response DTO |
| **application** | `application` | 业务用例编排、事务边界、跨聚合协调、领域事件处理、对外暴露应用服务 | ApplicationService、Command、应用层 DTO、EventListener |
| **domain** | `domain` | 业务规则与不变量、聚合根行为、领域事件定义、仓储接口 | 聚合根、值对象、领域服务、Repository 接口、DomainEvent |
| **infrastructure** | `infrastructure` | 技术实现细节（持久化、外部 SDK、文件 IO、消息中间件） | Repository 实现（MyBatis）、外部 API Adapter、Config |

### 2. 依赖方向

```
interfaces  ──→  application  ──→  domain
                     │
                     └────→  infrastructure  ──→  domain（实现 domain 定义的接口）
```

- 依赖**只能向内指向 `domain`**，反向严禁。
- `infrastructure` 通过实现 `domain` 中的 Repository 接口完成依赖反转，`application` 持有的是接口、不感知具体实现。
- 跨上下文（Video / Metadata / Distribution / Promotion）**禁止直接互相 import**，只能通过 Spring `ApplicationEvent` 传递领域事件通信。

### 3. 各层禁止事项

| 层 | ❌ 禁止 |
|---|---|
| **interfaces** | 写业务逻辑；直接操作 Repository、领域对象；持有事务 |
| **application** | 写复杂业务规则（应下沉到 domain）；返回领域对象给外部（必须转 DTO）；直接依赖具体 Repository 实现类 |
| **domain** | 引入 Spring / MyBatis / 任何框架注解；依赖 application、infrastructure、interfaces；调用外部服务 |
| **infrastructure** | 写业务逻辑；被 domain 反向 import |

### 4. DTO 类型对照

| DTO 类型 | 位置 | 用途 |
|---|---|---|
| Request DTO | `interfaces.rest.dto.request` | 接收 HTTP 请求体，承载 `@Valid` 校验注解 |
| Response DTO | `interfaces.rest.dto.response` | 序列化为 HTTP 响应，仅含展示字段 |
| Application DTO | `application.dto` | ApplicationService 的入参/出参，供 Controller 与跨上下文应用层调用使用 |
| Command | `application.command` | 写操作的入参对象（CQRS 中的 Command） |

> 转换原则：Controller 负责 `Request DTO ↔ Command/应用 DTO`、`应用 DTO → Response DTO` 的映射，**不要把 Application DTO 直接返回给前端**。

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
