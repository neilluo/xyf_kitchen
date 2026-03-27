# Phase 9: 前端核心页面（Dashboard + 视频管理 + 上传 + 元数据审核）

> 参考实施路线图：`docs/frontend/00-index.md` §实现顺序建议 4（前 4 个页面）
> 产出：Dashboard 页面、视频管理页面、视频上传页面、元数据审核页面

## 进度统计

- [ ] 共 10 个任务，已完成 8/10

---

## 任务列表

### P9-01: 实现 DashboardPage - StatsCard 组件

- **参考文档**: `docs/frontend/04-page-dashboard.md` §B（组件层级）、§G（StatsCard 视觉规范）
- **产出文件**:
  - `grace-frontend/src/pages/DashboardPage.tsx`（页面骨架 + StatsCardGrid）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-14, P8-11
- **状态**: [x]
- **注意**: 4 列 grid，每张卡片含 border-l-4 颜色区分（primary/orange/green/tertiary）；使用 useDashboardOverview hook；日期范围使用 useState 管理

---

### P9-02: 实现 DashboardPage - 完整页面

- **参考文档**: `docs/frontend/04-page-dashboard.md` §B-G 全部（RecentUploadsTable + DonutChart + PromotionOverview）
- **产出文件**:
  - `grace-frontend/src/pages/DashboardPage.tsx`（完善完整页面）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit && npm run lint`
- **依赖**: P9-01
- **状态**: [x]
- **注意**: RecentUploadsTable 行可点击跳转元数据审核；DonutChart 使用 Recharts PieChart 或 SVG 实现；PromotionOverview 渠道行含 ProgressBar；底部 Analytics 展示平均互动率和总曝光量；参考 HTML 原型 `ui/stitch_grace_video_management/grace_dashboard/code.html`

---

### P9-03: 实现 VideoManagementPage - FilterBar 与表格

- **参考文档**: `docs/frontend/05-page-video-management.md` §B-G 全部
- **产出文件**:
  - `grace-frontend/src/pages/VideoManagementPage.tsx`
- **验证命令**: `cd grace-frontend && npx tsc --noEmit && npm run lint`
- **依赖**: P8-14, P8-11
- **状态**: [x]
- **注意**: FilterBar 含搜索（防抖 300ms）+ 状态下拉 + 日期范围；VideoTable 含缩略图（hover 显示 play_arrow 覆层）、文件名+格式 badge、时长、大小、状态 StatusBadge、操作按钮（visibility/edit/send/more_vert）；分页组件；"上传新视频"按钮导航到 /upload；参考 HTML 原型

---

### P9-04: 实现 VideoUploadPage - DropZone

- **参考文档**: `docs/frontend/06-page-video-upload.md` §B（DropZone 组件树）、§G（DropZone 视觉规范）
- **产出文件**:
  - `grace-frontend/src/pages/VideoUploadPage.tsx`（页面骨架 + DropZone）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-14
- **状态**: [x]
- **注意**: DropZone 使用 SVG 虚线边框（不使用 CSS border-dashed）；支持 drag & drop + 点击选择；拖拽激活时 bg-primary/10；格式校验（MP4/MOV/AVI/MKV）+ 大小校验（5GB）在选择文件后立即执行

---

### P9-05: 实现 VideoUploadPage - 分片上传与进度

- **参考文档**: `docs/frontend/06-page-video-upload.md` §C（分片上传流程）、§D（速度计算）、§E-F（交互 + 错误处理）、§G（ProgressCard + CompletedItem 规范）
- **产出文件**:
  - `grace-frontend/src/pages/VideoUploadPage.tsx`（完善上传逻辑 + ProgressCard + CompletedList + TipCards）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit && npm run lint`
- **依赖**: P9-04, P8-11
- **状态**: [x]
- **注意**: 使用 useUpload hook 执行 init → chunk × N → complete 流程；UploadProgressCard 显示文件名/大小/进度条/百分比/速度/预估时间/取消按钮；CompletedUploadItem 显示 check_circle + "审核元数据"链接；分片失败自动重试 3 次；底部 EditorialTipCards（渐变 + tertiary）

---

### P9-06: 实现 MetadataReviewPage - 视频预览面板

- **参考文档**: `docs/frontend/07-page-metadata-review.md` §B（左侧 VideoPreviewCard）、§G（VideoPreviewCard 视觉规范）
- **产出文件**:
  - `grace-frontend/src/pages/MetadataReviewPage.tsx`（页面骨架 + 左侧视频预览）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-14, P8-11
- **状态**: [x]
- **注意**: 双栏布局 grid lg:grid-cols-2 gap-8；左侧含 aspect-video 视频预览（缩略图 + 播放按钮覆层 + 底部进度条）+ 视频信息网格（文件名/格式/大小/时长）；使用 useVideoDetail(videoId)

---

### P9-07: 实现 MetadataReviewPage - 元数据编辑器

- **参考文档**: `docs/frontend/07-page-metadata-review.md` §B（右侧 MetadataEditorCard）、§C-F（API + 交互 + 错误处理）、§G（编辑器视觉规范）
- **产出文件**:
  - `grace-frontend/src/pages/MetadataReviewPage.tsx`（完善右侧编辑器 + 全部交互）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit && npm run lint`
- **依赖**: P9-06
- **状态**: [x]
- **注意**: 编辑器含：AiBadge（auto_awesome 图标）、标题 Input（字符计数 /100）、描述 Textarea（字符计数 /5000）、TagChip 列表（可删除 + 添加）；操作栏：重新生成（C3）、保存草稿（C2）、确认元数据（C4，不可逆）；确认后编辑器变只读；glass-panel 毛玻璃效果可选；参考 HTML 原型

---

### P9-08: 前端 Lint + TypeScript 全量检查（Phase 9 验证）

- **参考文档**: `docs/frontend/01-tech-stack-and-conventions.md` §4（编码约定）
- **产出文件**: 无新增文件，修复上述任务中的 lint/type 问题
- **验证命令**: `cd grace-frontend && npm run lint && npx tsc --noEmit`
- **依赖**: P9-01 ~ P9-07
- **状态**: [x]
- **注意**: 确保全部页面组件通过 ESLint + TypeScript strict 检查；修复所有 warning 和 error

---

### P9-09: 配置 ESLint

- **参考文档**: `docs/frontend/01-tech-stack-and-conventions.md` §4
- **产出文件**:
  - `grace-frontend/.eslintrc.cjs` 或 `grace-frontend/eslint.config.js`
- **验证命令**: `cd grace-frontend && npm run lint`
- **依赖**: P8-01
- **状态**: [ ]
- **注意**: 配置 React + TypeScript ESLint 规则；确保 npm run lint 命令可用

---

### P9-10: 前端构建验证

- **参考文档**: 无
- **产出文件**: 无
- **验证命令**: `cd grace-frontend && npm run build`
- **依赖**: P9-08, P9-09
- **状态**: [ ]
- **注意**: 确保 Vite 生产构建成功，无编译错误
