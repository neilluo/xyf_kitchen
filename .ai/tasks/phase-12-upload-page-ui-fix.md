# Phase 12: 上传页面 UI 修复

> 参考实施路线图：复原 UI 原型 `ui/stitch_grace_video_management/grace_video_upload/code.html`
> 产出：修复 VideoUploadPage.tsx 以匹配 UI 原型设计

## 进度统计

- [x] 共 1 个任务，已完成 1/1

---

## 任务列表

### P12-01: 修复 VideoUploadPage UI 以匹配原型设计

- **参考文档**: 
  - `ui/stitch_grace_video_management/grace_video_upload/code.html`（UI 原型）
  - `docs/frontend/06-page-video-upload.md`（页面设计文档）
  - `api.md` §B（视频上传 API 契约）
- **产出文件**:
  - `grace-frontend/src/pages/VideoUploadPage.tsx`（修复后的页面）
- **验证命令**: `cd grace-frontend && npm run lint && npx tsc --noEmit`
- **依赖**: P9-04, P9-05（上传功能已实现）
- **状态**: [x]
- **注意**: 
  - 保持现有 API 集成逻辑不变（useInitUpload/useUploadChunk/useCompleteUpload）
  - 仅修改 UI 样式和布局以匹配原型

---

## UI 修复清单

### 1. 页面标题区域
**当前**: "上传视频" + 简单副标题
**目标**: "发布您的佳作" + "将您的烹饪灵感分享给世界，支持多平台一键分发。"

**修改点**:
- 主标题改为 `font-headline text-[2.75rem] font-bold`
- 副标题使用 `text-slate-500 mt-2 font-body`

### 2. DropZone 组件
**当前**: 只有拖拽区域，没有按钮
**目标**: 添加"选择文件"按钮（带 add_circle 图标）

**修改点**:
- 在 DropZone 内添加按钮：
  ```tsx
  <button className="px-8 py-3 rounded-lg border-2 border-primary text-primary font-semibold hover:bg-primary/5 transition-all flex items-center gap-2">
    <Icon name="add_circle" />
    选择文件
  </button>
  ```
- 按钮点击触发文件选择

### 3. 上传区域容器
**当前**: DropZone 直接放在页面上
**目标**: 使用 `bg-surface-container-lowest rounded-xl p-4` 包裹

**修改点**:
```tsx
<section className="bg-surface-container-lowest rounded-xl p-4 mb-8">
  <DropZone ... />
</section>
```

### 4. 上传进度卡片
**当前**: 基本样式正确
**目标**: 微调以匹配原型

**修改点**:
- 文件图标使用 `bg-secondary-container rounded-lg`
- 进度条上方添加 `mt-4`
- 速度信息使用 `flex items-center gap-1`

### 5. 已完成上传项
**当前**: 只显示"上传完成"
**目标**: 根据视频状态显示不同文本

**修改点**:
- UPLOADED 状态: "上传完成，正在生成元数据..."
- METADATA_GENERATED 状态: "元数据已生成，等待审核"
- READY_TO_PUBLISH 及以上: "上传完成"

### 6. 历史记录项（新增）
**当前**: 无历史记录展示
**目标**: 添加已发布视频的历史记录展示

**修改点**:
- 查询最近上传的视频列表（使用 useVideos hook）
- 显示平台发布状态图标（YT/FB/IG 等）
- 样式参考原型中的"家庭周末烘焙日"项

### 7. 底部提示卡片
**当前**: 已实现
**目标**: 检查样式一致性

**修改点**:
- 渐变卡片: `bg-gradient-to-br from-primary to-primary-container`
- tertiary 卡片: `bg-tertiary-fixed`
- 确保使用正确的颜色 token

---

## 数据结构

### CompletedUpload 扩展
```typescript
interface CompletedUpload {
  videoId: string
  fileName: string
  fileSize: number
  completedAt: Date
  status: VideoStatus  // 新增：用于显示不同状态文本
  thumbnailUrl?: string  // 新增：视频缩略图
  publishRecords?: { platform: string; status: string }[]  // 新增：发布记录
}
```

### 状态文本映射
```typescript
const statusTextMap: Record<VideoStatus, string> = {
  UPLOADED: '上传完成，正在生成元数据...',
  METADATA_GENERATED: '元数据已生成，等待审核',
  READY_TO_PUBLISH: '准备就绪，等待发布',
  PUBLISHING: '正在发布...',
  PUBLISHED: '已发布',
  PUBLISH_FAILED: '发布失败',
  PROMOTION_DONE: '推广完成',
}
```

---

## API 集成保持不变的 hooks

- `useInitUpload()` - 初始化上传
- `useUploadChunk()` - 上传分片
- `useCompleteUpload()` - 完成上传
- `useVideos()` - 获取视频列表（用于历史记录）

---

## 验证清单

- [x] 页面标题显示 "发布您的佳作"
- [x] DropZone 包含"选择文件"按钮
- [x] 上传区域有 `bg-surface-container-lowest` 背景
- [x] 上传进度卡片样式匹配原型
- [x] 已完成项根据状态显示不同文本
- [x] 历史记录项显示平台图标
- [x] 底部提示卡片样式正确
- [x] `npm run lint` 通过
- [x] `npx tsc --noEmit` 通过
