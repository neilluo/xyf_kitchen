# Phase 8: 前端基础设施与设计系统

> 参考实施路线图：`docs/frontend/00-index.md` §实现顺序建议 1-3
> 产出：Vite 项目初始化、Tailwind 设计系统配置、11 个原子 UI 组件、3 个布局组件、API 客户端、类型定义、工具函数、Zustand Store、路由配置

## 进度统计

- [ ] 共 14 个任务，已完成 2/14

---

## 任务列表

### P8-01: 初始化 Vite + React + TypeScript 项目

- **参考文档**: `docs/frontend/01-tech-stack-and-conventions.md` §1（项目初始化命令 + 核心依赖）
- **产出文件**:
  - `grace-frontend/package.json`
  - `grace-frontend/vite.config.ts`
  - `grace-frontend/tsconfig.json`
  - `grace-frontend/index.html`（含 Google Fonts + Material Symbols 引入）
  - `grace-frontend/.env`
- **验证命令**: `cd grace-frontend && npm install && npx tsc --noEmit`
- **依赖**: 无
- **状态**: [x]
- **注意**: 安装全部核心依赖（react-router-dom@6, @tanstack/react-query@5, zustand@4, axios@1, recharts@2, tailwindcss@3, postcss, autoprefixer, @tailwindcss/forms）；Vite server 端口 3000，proxy /api → localhost:8080

---

### P8-02: 配置 Tailwind CSS 设计系统

- **参考文档**: `docs/frontend/02-design-system.md` §2（完整 tailwind.config.ts）、§1（设计原则 + Surface 层级体系）
- **产出文件**:
  - `grace-frontend/tailwind.config.ts`
  - `grace-frontend/src/index.css`（Tailwind 指令 + Material Symbols font-variation-settings）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit && npm run build`
- **依赖**: P8-01
- **状态**: [x]
- **注意**: 包含全部自定义颜色 token（primary/secondary/tertiary/error/surface 系列）、fontFamily（Manrope/Inter）、自定义 borderRadius；index.css 仅含 @tailwind 指令和 Material Symbols 基线样式

---

### P8-03: 实现 Icon 组件

- **参考文档**: `docs/frontend/02-design-system.md` §4（Icon 组件规范 + 图标列表）
- **产出文件**:
  - `grace-frontend/src/components/ui/Icon.tsx`
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-02
- **状态**: [x]
- **注意**: 封装 Google Material Symbols Outlined；Props: name(string), className?(string), size?(number, default 20)

---

### P8-04: 实现 Button 组件

- **参考文档**: `docs/frontend/02-design-system.md` §5.1（5 种 Button 变体规范）
- **产出文件**:
  - `grace-frontend/src/components/ui/Button.tsx`
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-03
- **状态**: [x]
- **注意**: 5 种变体：Primary(渐变 CTA)、Secondary、Ghost、Danger、Icon Button；Props: variant, children, icon?, disabled?, onClick, className?；Primary 使用 bg-gradient-to-r from-primary to-primary-container

---

### P8-05: 实现 StatusBadge、Card、Input、Select 组件

- **参考文档**: `docs/frontend/02-design-system.md` §5.2-5.5（StatusBadge 10 种状态映射 + Card/Input/Select 规范）
- **产出文件**:
  - `grace-frontend/src/components/ui/StatusBadge.tsx`
  - `grace-frontend/src/components/ui/Card.tsx`
  - `grace-frontend/src/components/ui/Input.tsx`
  - `grace-frontend/src/components/ui/Select.tsx`
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-02
- **状态**: [ ]
- **注意**: StatusBadge 需映射 VideoStatus/PromotionStatus/PublishStatus 到对应背景和文字色；Card 遵循 No-Line 规则（无 border/shadow）；Input 无 border，focus 时 ring-2 ring-primary/40

---

### P8-06: 实现 TagChip、ProgressBar、Pagination、Toggle、Table 组件

- **参考文档**: `docs/frontend/02-design-system.md` §5.6-5.9（TagChip/ProgressBar/Pagination/Toggle 规范）
- **产出文件**:
  - `grace-frontend/src/components/ui/TagChip.tsx`
  - `grace-frontend/src/components/ui/ProgressBar.tsx`
  - `grace-frontend/src/components/ui/Pagination.tsx`
  - `grace-frontend/src/components/ui/Toggle.tsx`
  - `grace-frontend/src/components/ui/Table.tsx`
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-03
- **状态**: [ ]
- **注意**: TagChip 可选 onRemove 回调（显示 close 图标）；Pagination 含 page/totalPages/onPageChange；Toggle 基于 checkbox peer 模式；Table 为 thead/tbody 基础封装

---

### P8-07: 实现 AppLayout + Sidebar + Header 布局组件

- **参考文档**: `docs/frontend/02-design-system.md` §6-7（侧边栏导航结构 + 全局布局）、`docs/frontend/03-shared-infrastructure.md` §6（布局组件规范）
- **产出文件**:
  - `grace-frontend/src/components/layout/AppLayout.tsx`
  - `grace-frontend/src/components/layout/Sidebar.tsx`
  - `grace-frontend/src/components/layout/Header.tsx`
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-03, P8-04
- **状态**: [ ]
- **注意**: Sidebar 宽 240px，bg-[#001529]，7 个导航项（useLocation 高亮），底部设置按钮；Header 毛玻璃效果 bg-white/80 backdrop-blur-md；AppLayout 使用 React Router `<Outlet />`

---

### P8-08: 创建通用类型定义

- **参考文档**: `docs/frontend/03-shared-infrastructure.md` §2（类型定义 + 枚举类型示例）
- **产出文件**:
  - `grace-frontend/src/types/common.ts`（ApiResponse, PaginatedData, PaginationParams）
  - `grace-frontend/src/types/video.ts`
  - `grace-frontend/src/types/metadata.ts`
  - `grace-frontend/src/types/distribution.ts`
  - `grace-frontend/src/types/channel.ts`
  - `grace-frontend/src/types/promotion.ts`
  - `grace-frontend/src/types/settings.ts`
  - `grace-frontend/src/types/dashboard.ts`
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-01
- **状态**: [ ]
- **注意**: 严格按 api.md 中定义的 API 请求/响应结构编写；VideoStatus 7 种、PromotionStatus 4 种、PublishStatus 5 种

---

### P8-09: 实现 API 客户端层

- **参考文档**: `docs/frontend/03-shared-infrastructure.md` §1（Axios 实例 + 拦截器 + ApiError 类）
- **产出文件**:
  - `grace-frontend/src/api/client.ts`（Axios 实例 + 响应拦截器 + ApiError 类）
  - `grace-frontend/src/utils/errorMessages.ts`（错误码映射表）
  - `grace-frontend/src/utils/validation.ts`（客户端校验规则 + 消息）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-08
- **状态**: [ ]
- **注意**: 响应拦截器需解包 ApiResponse 信封，code !== 0 时 reject 为 ApiError；包含全部错误码（1001-9999）映射

---

### P8-10: 创建领域 API 请求函数

- **参考文档**: `docs/frontend/03-shared-infrastructure.md` §1、`api.md` 各端点
- **产出文件**:
  - `grace-frontend/src/api/dashboard.ts`（A1）
  - `grace-frontend/src/api/video.ts`（B1-B6）
  - `grace-frontend/src/api/metadata.ts`（C1-C5）
  - `grace-frontend/src/api/distribution.ts`（D1-D6）
  - `grace-frontend/src/api/channel.ts`（E1-E5）
  - `grace-frontend/src/api/promotion.ts`（F1-F5）
  - `grace-frontend/src/api/settings.ts`（G1-G10）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-09
- **状态**: [ ]
- **注意**: 每个文件导出与 api.md 对应的请求函数；上传分片使用 multipart/form-data；所有函数通过 apiClient 调用

---

### P8-11: 实现 React Query Hooks

- **参考文档**: `docs/frontend/03-shared-infrastructure.md` §3（Hook 模式 + queryKey 常量 + 各领域 Hook 清单 + 轮询模式）
- **产出文件**:
  - `grace-frontend/src/hooks/useDashboard.ts`
  - `grace-frontend/src/hooks/useVideos.ts`
  - `grace-frontend/src/hooks/useUpload.ts`（分片上传 hook + 进度轮询）
  - `grace-frontend/src/hooks/useMetadata.ts`
  - `grace-frontend/src/hooks/useDistribution.ts`
  - `grace-frontend/src/hooks/useChannels.ts`
  - `grace-frontend/src/hooks/usePromotions.ts`
  - `grace-frontend/src/hooks/useSettings.ts`
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-10
- **状态**: [ ]
- **注意**: 每个文件导出 queryKeys 常量 + 查询/变更 hooks；useUpload 含分片上传核心逻辑 + 速度计算；usePublishStatus 和 useUploadProgress 使用 refetchInterval 轮询

---

### P8-12: 实现 Zustand Store

- **参考文档**: `docs/frontend/03-shared-infrastructure.md` §4（AppStore 定义）
- **产出文件**:
  - `grace-frontend/src/store/useAppStore.ts`
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-01
- **状态**: [ ]
- **注意**: 管理 Toast 通知队列 + 上传队列；Toast 含 id/type/message；上传队列项含 file/uploadId/progress/status

---

### P8-13: 实现工具函数

- **参考文档**: `docs/frontend/03-shared-infrastructure.md` §7（格式化函数 + 状态映射）
- **产出文件**:
  - `grace-frontend/src/utils/format.ts`（formatFileSize, formatDuration, formatDate, formatPercent）
  - `grace-frontend/src/utils/status.ts`（VIDEO_STATUS_MAP, PROMOTION_STATUS_MAP, PUBLISH_STATUS_MAP）
  - `grace-frontend/src/utils/constants.ts`（路由路径常量）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-08
- **状态**: [ ]
- **注意**: formatDuration 处理 ISO 8601 Duration（PT12M34S → 12:34）；状态映射需包含 label/bgClass/textClass

---

### P8-14: 配置路由与主入口

- **参考文档**: `docs/frontend/03-shared-infrastructure.md` §5（路由配置 + QueryClient 默认选项）
- **产出文件**:
  - `grace-frontend/src/App.tsx`
  - `grace-frontend/src/main.tsx`
- **验证命令**: `cd grace-frontend && npm run build`
- **依赖**: P8-07, P8-12
- **状态**: [ ]
- **注意**: QueryClient staleTime 1 分钟，retry 1，refetchOnWindowFocus false；7 个路由全部在 AppLayout 下；暂用空页面占位（后续 Phase 9-10 实现）
