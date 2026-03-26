# Grace 前端技术文档索引

## 项目简介

Grace 是面向美食博主的视频分发与推广平台。用户可上传视频、由 AI 自动生成元数据、审核后发布到 YouTube 等平台，并通过 OpenCrawl 在社交媒体自动推广。

## 技术栈汇总

| 类别 | 选择 | 版本 |
|------|------|------|
| 框架 | React + TypeScript | React 18, TS 5.x |
| 构建工具 | Vite | 5.x |
| 样式 | Tailwind CSS（自定义 token） | v3 |
| 组件库 | 自建原子组件（不使用第三方 UI 库） | - |
| 路由 | React Router | v6 |
| 服务端状态 | TanStack Query (React Query) | v5 |
| 客户端状态 | Zustand | v4 |
| HTTP 客户端 | Axios | 1.x |
| 图表 | Recharts | 2.x |
| 图标 | Google Material Symbols Outlined | - |
| 字体 | Manrope（标题）+ Inter（正文） | Google Fonts |

## 文档导航

| 文档 | 文件 | 说明 |
|------|------|------|
| 技术栈与编码约定 | [01-tech-stack-and-conventions.md](./01-tech-stack-and-conventions.md) | Vite 配置、目录结构、文件命名、环境变量 |
| 设计系统 | [02-design-system.md](./02-design-system.md) | 完整 Tailwind 配置、颜色 token、字体、组件原语规范 |
| 共享基础设施 | [03-shared-infrastructure.md](./03-shared-infrastructure.md) | API 客户端、类型定义、React Query hooks、布局组件、路由 |
| 仪表盘 | [04-page-dashboard.md](./04-page-dashboard.md) | Dashboard 页面规范 |
| 视频管理 | [05-page-video-management.md](./05-page-video-management.md) | 视频列表页面规范 |
| 视频上传 | [06-page-video-upload.md](./06-page-video-upload.md) | 分片上传页面规范 |
| 元数据审核 | [07-page-metadata-review.md](./07-page-metadata-review.md) | AI 元数据编辑页面规范 |
| 分发与推广 | [08-page-distribution-promotion.md](./08-page-distribution-promotion.md) | 3 步向导页面规范 |
| 推广历史 | [09-page-promotion-history.md](./09-page-promotion-history.md) | 推广记录页面规范 |
| 设置 | [10-page-settings.md](./10-page-settings.md) | 用户设置页面规范 |

## 页面-API-组件映射表

此表是 AI 检索的核心入口，快速定位每个页面使用的 API 端点和关键组件。

| 页面 | 路由 | 文档 | API 端点 | 关键组件 |
|------|------|------|----------|----------|
| 仪表盘 | `/` | 04 | A1 | StatsCard, RecentUploadsTable, DonutChart, PromotionOverview |
| 视频管理 | `/videos` | 05 | B5, B6 | FilterBar, VideoTable, StatusBadge, Pagination |
| 视频上传 | `/upload` | 06 | B1, B2, B3, B4 | DropZone, UploadProgressCard, CompletedList |
| 元数据审核 | `/videos/:videoId/metadata` | 07 | B6, C1-C5 | VideoPreview, MetadataEditor, TagChipInput, AiBadge |
| 分发与推广 | `/videos/:videoId/distribute` | 08 | B6, D1, D2, D5, F1, F2 | StepWizard, PlatformCard, PromotionCopyCard |
| 推广历史 | `/promotions` | 09 | F3, F4, F5 | FilterBar, ExpandableTable, InsightCard, StatusBadge |
| 设置 | `/settings` | 10 | G1-G10, E4 | ProfileCard, ConnectedAccounts, NotificationToggles, ApiKeyManager |

> **注：** E1-E5（推广渠道 CRUD）端点由设置页面和分发向导页面间接使用。E4（列出渠道）在分发向导 Step 3 生成推广文案时用于获取可用渠道列表，渠道管理功能整合在设置页面中。D3/D4（OAuth 授权）由设置页面的"连接账户"功能调用。

## 文档依赖关系

```
00-index.md (本文件)
├── 01-tech-stack-and-conventions.md  ← 项目脚手架时首先阅读
├── 02-design-system.md              ← 所有页面文档的视觉基准
├── 03-shared-infrastructure.md      ← 所有页面文档的技术基准
└── 04~10 页面文档                    ← 各自依赖 02 和 03
```

**实现顺序建议：**

1. 项目脚手架初始化（参考 `01`）
2. 配置 Tailwind + 实现设计系统原子组件（参考 `02`）
3. 实现布局组件 + API 客户端 + 类型定义 + 路由（参考 `03`）
4. 按页面逐个实现：Dashboard → 视频管理 → 上传 → 元数据审核 → 分发推广 → 推广历史 → 设置

## 后端参考

| 文件 | 说明 |
|------|------|
| `design.md` | 后端 DDD 架构、领域模型、事件、错误处理 |
| `requirements.md` | 10 个需求及验收标准 |
| `api.md` | 30+ REST API 完整契约（本前端的接口依据） |

## UI 设计稿参考

| 页面 | 截图 | HTML 原型 |
|------|------|-----------|
| 仪表盘 | `ui/stitch_grace_video_management/grace_dashboard/screen.png` | `grace_dashboard/code.html` |
| 视频管理 | `ui/stitch_grace_video_management/grace_video_management/screen.png` | `grace_video_management/code.html` |
| 视频上传 | `ui/stitch_grace_video_management/grace_video_upload/screen.png` | `grace_video_upload/code.html` |
| 元数据审核 | `ui/stitch_grace_video_management/grace_metadata_review/screen.png` | `grace_metadata_review/code.html` |
| 分发与推广 | `ui/stitch_grace_video_management/grace_distribution_promotion/screen.png` | `grace_distribution_promotion/code.html` |
| 推广历史 | `ui/stitch_grace_video_management/grace_promotion_history/screen.png` | `grace_promotion_history/code.html` |
| 设置 | `ui/stitch_grace_video_management/grace_settings_page_updated_sidebar/screen.png` | `grace_settings_page_updated_sidebar/code.html` |
| 设计系统 | - | `ui/stitch_grace_video_management/grace_minimalist/DESIGN.md` |
