# Grace Platform

面向美食博主的视频分发与推广平台。博主上传本地视频后，由 AI（阿里云通义千问）自动生成元数据（标题/描述/标签），经用户审核确认后发布到 YouTube，并集成 OpenCrawl 在社交媒体和论坛自动执行推广。

## Tech Stack

| 层 | 技术 | 版本 |
|----|------|------|
| Backend | Java + Spring Boot + Maven | 21 / 3.4.1 / 3.9+ |
| Database | MySQL + MyBatis + Flyway | 8.0+ |
| Frontend | React + TypeScript + Vite | 18 / 5.x / 5.x |
| Styling | Tailwind CSS（自定义 token，自建原子组件） | v3 |
| State | TanStack Query + Zustand | v5 / v4 |
| Testing | JUnit 5 + jqwik + Mockito + Testcontainers | — |
| External | 通义千问 API, YouTube Data API v3, OpenCrawl | — |

## Quick Start

### Prerequisites

- Java 21
- Node.js 18+
- Maven 3.9+
- MySQL 8.0+（阿里云 RDS 或本地实例）

### 1. Configure Environment

```bash
cp .env.example .env
# 编辑 .env，填入必填项：
#   MYSQL_HOST / MYSQL_PASSWORD（阿里云 RDS 连接信息）
#   GRACE_ENCRYPTION_KEY (openssl rand -base64 32)
#   QWEN_API_KEY
#   YOUTUBE_CLIENT_ID / YOUTUBE_CLIENT_SECRET
#   OPENCRAWL_API_KEY
```

### 2. Start Backend

```bash
cd grace-platform
mvn spring-boot:run
# 后端运行在 http://localhost:8080
```

### 3. Start Frontend

```bash
cd grace-frontend
npm install
npm run dev
# 前端运行在 http://localhost:3000
```

## Project Structure

```
grace/
├── AGENTS.md                    # AI agent 指令文件
├── requirements.md              # 10 个需求 + 验收标准
├── design.md                    # DDD 技术架构设计
├── api.md                       # 30+ REST API 契约
├── docs/
│   ├── backend/                 # 后端实现指南（13 份文档）
│   │   ├── 00-index.md          # 总入口 + 实施路线图
│   │   ├── 01~10               # 脚手架 → 测试策略
│   │   ├── db-design.md         # 数据库设计规范
│   │   └── log-design.md        # 日志规范
│   └── frontend/                # 前端实现指南（11 份文档）
│       ├── 00-index.md          # 总入口
│       └── 01~10               # 技术栈 → 各页面规范
├── ui/                          # UI 原型（HTML + 截图）
├── grace-platform/              # 后端源码（Java/Spring Boot）
└── grace-frontend/              # 前端源码（React/TypeScript）
```

## Documentation

| 文档 | 说明 |
|------|------|
| [requirements.md](./requirements.md) | 需求与验收标准 |
| [design.md](./design.md) | DDD 架构、领域模型、设计模式 |
| [api.md](./api.md) | REST API 完整契约 |
| [docs/backend/00-index.md](./docs/backend/00-index.md) | 后端实现指南入口 |
| [docs/frontend/00-index.md](./docs/frontend/00-index.md) | 前端实现指南入口 |
| [docs/backend/db-design.md](./docs/backend/db-design.md) | 数据库设计规范 |
| [docs/backend/10-testing-strategy.md](./docs/backend/10-testing-strategy.md) | 测试策略 |

## Architecture

采用 DDD（领域驱动设计），划分为 5 个限界上下文：

1. **Video** — 分片上传、文件管理
2. **Metadata** — AI 生成元数据、用户审核编辑
3. **Distribution** — 多平台发布（MVP: YouTube）
4. **Promotion** — OpenCrawl 社交推广
5. **User/Settings** — 用户资料、OAuth 连接、通知、API Key

上下文间通过领域事件通信：`VideoUploadedEvent` → `MetadataConfirmedEvent` → `VideoPublishedEvent`
