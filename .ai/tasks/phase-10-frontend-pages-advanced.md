# Phase 10: 前端高级页面（分发推广 + 推广历史 + 设置）

> 参考实施路线图：`docs/frontend/00-index.md` §实现顺序建议 4（后 3 个页面）
> 产出：分发与推广向导页面、推广历史页面、设置页面

## 进度统计

- [ ] 共 10 个任务，已完成 0/10

---

## 任务列表

### P10-01: 实现 DistributionPromotionPage - StepWizard 骨架

- **参考文档**: `docs/frontend/08-page-distribution-promotion.md` §B（StepWizard + StepIndicator 组件树）
- **产出文件**:
  - `grace-frontend/src/pages/DistributionPromotionPage.tsx`（页面骨架 + Step 导航逻辑）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-14, P8-11
- **状态**: [ ]
- **注意**: 3 步向导：Step 1（确认信息）→ Step 2（选择平台）→ Step 3（推广配置）；StepIndicator 使用圆点 + 连接线，支持 completed/active/future 三态；使用 useState 管理当前步骤

---

### P10-02: 实现 DistributionPromotionPage - Step 1 & Step 2

- **参考文档**: `docs/frontend/08-page-distribution-promotion.md` §B（Step 1 VideoConfirmation + Step 2 PlatformSelection）、§C（API 端点 B6/D5）
- **产出文件**:
  - `grace-frontend/src/pages/DistributionPromotionPage.tsx`（完善 Step 1 + Step 2）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P10-01
- **状态**: [ ]
- **注意**: Step 1 展示视频缩略图 + 元数据摘要（标题/描述/标签）；Step 2 使用 usePlatforms() 加载平台列表，grid-cols-4 PlatformCard（YouTube active，其他 coming soon 禁用），含 PrivacyStatusSelect；参考 HTML 原型

---

### P10-03: 实现 DistributionPromotionPage - Step 3 与发布流程

- **参考文档**: `docs/frontend/08-page-distribution-promotion.md` §B（Step 3 PromotionConfig）、§C（完整发布+推广流程 D1/D2/F1/F2）
- **产出文件**:
  - `grace-frontend/src/pages/DistributionPromotionPage.tsx`（完善 Step 3 + 发布逻辑）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit && npm run lint`
- **依赖**: P10-02
- **状态**: [ ]
- **注意**: Step 3 进入时自动调用 F1 生成推广文案；PromotionCopyCard grid-cols-3 显示渠道图标/推广方式 badge/可编辑标题+正文/重新生成按钮；确认发布：调用 D1 发布 → 轮询 D2 状态 → 同时调用 F2 执行推广 → 显示结果摘要

---

### P10-04: 实现 PromotionHistoryPage - 筛选与表格

- **参考文档**: `docs/frontend/09-page-promotion-history.md` §B-C（组件树 + API 端点 F3/F4/F5/B5）
- **产出文件**:
  - `grace-frontend/src/pages/PromotionHistoryPage.tsx`（页面骨架 + FilterBar + PromotionTable）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-14, P8-11
- **状态**: [ ]
- **注意**: FilterBar 含视频选择下拉（useVideoList 加载）+ 日期范围 + 导出按钮；PromotionTable 按视频分组，SummaryRow 可展开（chevron 切换），含视频标题/渠道数/圆形 SVG 进度/状态/日期

---

### P10-05: 实现 PromotionHistoryPage - 展开详情与 Insight 卡片

- **参考文档**: `docs/frontend/09-page-promotion-history.md` §B（ExpandedDetail + InsightCards）
- **产出文件**:
  - `grace-frontend/src/pages/PromotionHistoryPage.tsx`（完善展开详情 + InsightCards + 分页）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit && npm run lint`
- **依赖**: P10-04
- **状态**: [ ]
- **注意**: ExpandedDetail 每行显示渠道图标/渠道名/推广方式 badge/状态/结果链接（open_in_new）/失败时显示重试按钮（F5）；InsightCards 3 列 bento 布局（成功率 tertiary-fixed、最佳方式 secondary-fixed、高峰时段 surface-container-high）；Pagination 组件

---

### P10-06: 实现 SettingsPage - 用户资料与头像

- **参考文档**: `docs/frontend/10-page-settings.md` §B（ProfileSection）、§C（G1/G2/G3 端点）
- **产出文件**:
  - `grace-frontend/src/pages/SettingsPage.tsx`（页面骨架 + 12 列 grid + ProfileSection）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P8-14, P8-11
- **状态**: [ ]
- **注意**: 12 列网格布局（左 col-span-7 右 col-span-5）；ProfileSection 含头像上传（w-32 h-32 rounded-full + ring-4 + CameraOverlay）+ displayName/email 表单；使用 useProfile/useUpdateProfile/useUploadAvatar hooks

---

### P10-07: 实现 SettingsPage - 连接账户与通知偏好

- **参考文档**: `docs/frontend/10-page-settings.md` §B（ConnectedAccountsSection + NotificationSection）、§C（G4/G5/D3/G6/G7 端点）
- **产出文件**:
  - `grace-frontend/src/pages/SettingsPage.tsx`（完善连接账户 + 通知偏好）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit`
- **依赖**: P10-06
- **状态**: [ ]
- **注意**: ConnectedAccounts 列出平台，已连接显示 check_circle 绿色 + 账户名，未连接显示"连接"按钮（触发 D3 OAuth）；NotificationSection 3 行通知项，每行 Label + Description + Toggle 开关；Toggle 变更立即调用 G7

---

### P10-08: 实现 SettingsPage - API Key 管理

- **参考文档**: `docs/frontend/10-page-settings.md` §B（ApiKeySection + ProTipCard）、§C（G8/G9/G10 端点）
- **产出文件**:
  - `grace-frontend/src/pages/SettingsPage.tsx`（完善右侧 API Key 管理 + ProTipCard）
- **验证命令**: `cd grace-frontend && npx tsc --noEmit && npm run lint`
- **依赖**: P10-07
- **状态**: [ ]
- **注意**: ApiKeyCard 显示遮蔽的 Key（••••••••abcd）+ 复制按钮 + 查看按钮 + 过期时间 + 最后使用时间 + 重新生成按钮；使用 useApiKeys/useCreateApiKey/useDeleteApiKey hooks；ProTipCard 含 auto_awesome 图标 + 使用提示

---

### P10-09: 前端全量 Lint + TypeScript + 构建验证

- **参考文档**: 无
- **产出文件**: 无新增文件，修复所有 lint/type 问题
- **验证命令**: `cd grace-frontend && npm run lint && npx tsc --noEmit && npm run build`
- **依赖**: P10-01 ~ P10-08
- **状态**: [ ]
- **注意**: 确保全部 7 个页面通过 ESLint + TypeScript strict + Vite 构建

---

### P10-10: 前端整体冒烟验证

- **参考文档**: 无
- **产出文件**: 无
- **验证命令**: `cd grace-frontend && npm run build && npm run lint && npx tsc --noEmit`
- **依赖**: P10-09
- **状态**: [ ]
- **注意**: 最终验证：确认所有页面组件正确导入、路由正常、类型无误、构建产物生成成功
