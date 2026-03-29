# UI 设计稿 vs 实际实现 - 完整对比分析

**分析日期:** 2026-03-29  
**分析人:** Kelly 🔬

---

## 1. Sidebar 导航对比

### UI 设计稿中的导航项

| 图标 | 文本 | 说明 |
|------|------|------|
| dashboard | 仪表盘 | Dashboard 页面 |
| video_library | 视频管理 | 视频列表页面 |
| fact_check | 元数据审核 | 需要 videoId 参数 |
| publish | 视频发布 | 需要 videoId 参数 |
| alt_route | 推广渠道 | 推广管理页面 |
| task | 推广任务 | 推广任务页面 |
| settings | 设置 | 设置页面（底部独立区域） |

### 实际实现 (Sidebar.tsx)

```typescript
const navItems: NavItem[] = [
  { icon: 'dashboard', label: '仪表盘', path: '/' },
  { icon: 'video_library', label: '视频管理', path: '/videos' },
  { icon: 'fact_check', label: '元数据审核', path: '/videos/metadata' },     // ❌
  { icon: 'publish', label: '视频发布', path: '/videos/distribute' },         // ❌
  { icon: 'alt_route', label: '推广渠道', path: '/promotions' },
  { icon: 'task', label: '推广任务', path: '/promotions/tasks' },            // ❌
  { icon: 'settings', label: '设置', path: '/settings' },
]
```

### 问题分析

1. **元数据审核** (`/videos/metadata`) - ❌ 需要 videoId 参数，不应该在 Sidebar 有直接链接
2. **视频发布** (`/videos/distribute`) - ❌ 需要 videoId 参数，不应该在 Sidebar 有直接链接
3. **推广任务** (`/promotions/tasks`) - ❌ 此路由不存在
4. **缺失：上传视频** - ❌ 应该有 `/upload` 的导航链接

---

## 2. 路由配置对比

### 实际路由 (App.tsx)

```typescript
<Route index element={<DashboardPage />} />                          // /
<Route path="videos" element={<VideoManagementPage />} />            // /videos
<Route path="upload" element={<VideoUploadPage />} />                // /upload ✅
<Route path="videos/:videoId/metadata" element={<MetadataReviewPage />} />  // 需要 videoId
<Route path="videos/:videoId/distribute" element={<DistributionPromotionPage />} /> // 需要 videoId
<Route path="promotions" element={<PromotionHistoryPage />} />       // /promotions
<Route path="settings" element={<SettingsPage />} />                 // /settings
```

### 问题

- ✅ `/upload` 路由存在，但 Sidebar 没有链接
- ❌ Sidebar 链接了需要 videoId 的页面
- ❌ `/promotions/tasks` 路由不存在

---

## 3. 页面状态对比

| 页面 URL | UI 设计稿 | 实际实现 | 状态 |
|---------|---------|---------|------|
| `/` (Dashboard) | ✅ 正常 | ✅ 正常 | ✅ OK |
| `/videos` (视频管理) | ✅ 正常 | ❌ 有问题 | ❌ BUG |
| `/upload` (视频上传) | ✅ 正常 | ❌ 有问题 | ❌ BUG |
| `/videos/:videoId/metadata` (元数据审核) | ✅ 正常 | ❌ 有问题 | ❌ BUG |
| `/videos/:videoId/distribute` (分发推广) | ✅ 正常 | ❌ 有问题 | ❌ BUG |
| `/promotions` (推广历史) | ✅ 正常 | ❌ 有问题 | ❌ BUG |
| `/settings` (设置) | ✅ 正常 | ❌ 有问题 | ❌ BUG |

---

## 4. 用户流程对比

### 正确的用户流程 (UI 设计稿)

```
1. 上传视频
   Sidebar → 无直接链接 (应该从视频管理进入？需要确认)
   实际：/upload

2. 查看视频列表
   Sidebar → 视频管理 → /videos

3. 审核元数据
   视频管理 → 点击具体视频 → /videos/{videoId}/metadata

4. 分发推广
   元数据审核完成 → /videos/{videoId}/distribute

5. 查看推广记录
   Sidebar → 推广渠道 → /promotions
```

### 当前实现的问题

1. Sidebar 有"元数据审核"直接链接，但路由需要 videoId
2. Sidebar 有"视频发布"直接链接，但路由需要 videoId
3. Sidebar 有"推广任务"链接，但路由不存在
4. 缺少"上传视频"的 Sidebar 链接

---

## 5. 建议修复方案

### 方案 A: 修改 Sidebar 导航（推荐）

```typescript
const navItems: NavItem[] = [
  { icon: 'dashboard', label: '仪表盘', path: '/' },
  { icon: 'video_library', label: '视频管理', path: '/videos' },
  { icon: 'cloud_upload', label: '上传视频', path: '/upload' }, // 新增
  { icon: 'alt_route', label: '推广历史', path: '/promotions' }, // 改名
  { icon: 'settings', label: '设置', path: '/settings' },
]
```

**说明:**
- 移除"元数据审核"和"视频发布"（这两个应该从视频管理页面跳转）
- 新增"上传视频"链接
- "推广渠道"改名为"推广历史"
- 移除"推广任务"

### 方案 B: 修改路由配置

保持 Sidebar 不变，修改路由支持无 videoId 访问：

```typescript
<Route path="videos/metadata" element={<MetadataReviewPage />} />
<Route path="videos/distribute" element={<DistributionPromotionPage />} />
<Route path="promotions/tasks" element={<PromotionHistoryPage />} />
```

**问题:** 页面需要 videoId 才能工作，此方案不可行。

---

## 6. 待确认问题

1. **上传视频入口** - UI 设计稿中 Sidebar 没有"上传视频"链接，但实际有 `/upload` 路由
   - 可能的设计：从视频管理页面的"上传新视频"按钮进入
   - 需要查看视频管理页面 UI 设计稿

2. **元数据审核和分发的入口** - 应该从视频管理页面的操作按钮进入
   - 需要查看视频管理页面 UI 设计稿是否有这些操作按钮

3. **推广渠道 vs 推广历史** - 名称不一致
   - UI 设计稿：推广渠道
   - 实际功能：查看推广历史记录
   - 需要确认正确名称

---

## 7. 下一步行动

1. 详细查看视频管理页面 UI 设计稿，确认操作按钮
2. 详细查看上传视频页面 UI 设计稿，确认入口位置
3. 创建 comprehensive issue 列出所有问题
4. 建议修复方案：修改 Sidebar 导航配置

---

**分析结论:** 除了 Dashboard 页面外，所有页面都存在路由或导航配置问题，需要系统性修复。
