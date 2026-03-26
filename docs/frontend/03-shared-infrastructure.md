# 共享基础设施

> 依赖文档：[01-tech-stack-and-conventions.md](./01-tech-stack-and-conventions.md)、[02-design-system.md](./02-design-system.md)
> 被依赖：所有页面文档 04-10

## 1. API 客户端层

### 1.1 Axios 实例

```typescript
// src/api/client.ts
import axios from 'axios'
import type { ApiResponse } from '@/types/common'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 响应拦截器：解包统一信封
apiClient.interceptors.response.use(
  (response) => {
    const data = response.data as ApiResponse<unknown>
    if (data.code !== 0) {
      return Promise.reject(new ApiError(data.code, data.message, data.errors))
    }
    return response
  },
  (error) => {
    if (error.response) {
      const data = error.response.data
      return Promise.reject(new ApiError(data?.code ?? 9999, data?.message ?? '网络错误', data?.errors))
    }
    return Promise.reject(new ApiError(9999, '网络连接失败'))
  }
)

export default apiClient
```

### 1.2 错误类型

```typescript
// src/api/client.ts (续)
export class ApiError extends Error {
  code: number
  errors?: FieldError[]

  constructor(code: number, message: string, errors?: FieldError[]) {
    super(message)
    this.code = code
    this.errors = errors
  }
}

interface FieldError {
  field: string
  message: string
}
```

### 1.3 错误码到用户提示的映射

```typescript
// src/utils/errorMessages.ts
export const ERROR_MESSAGES: Record<number, string> = {
  1001: '不支持的视频格式，请上传 MP4、MOV、AVI 或 MKV 文件',
  1002: '视频文件超过 5GB 限制',
  1003: '上传会话不存在或已过期，请重新上传',
  1004: '上传会话已超时，请重新上传',
  1005: '分片索引超出范围',
  1006: '重复上传分片',
  1007: '尚有分片未上传完成',
  1008: '视频不存在',
  2001: '元数据校验失败，请检查标题和描述',
  2002: '元数据不存在',
  2003: '元数据已确认，无法再编辑',
  2004: '视频尚未上传完成',
  3001: '不支持的分发平台',
  3002: '平台授权已过期，请重新连接',
  3003: '平台未授权，请先完成连接',
  3005: '视频尚未就绪，请先确认元数据',
  3006: '发布任务不存在',
  3007: '平台 API 异常，请稍后重试',
  4001: '推广渠道不存在',
  4002: '渠道配置无效',
  4003: '推广渠道已被禁用',
  4004: '推广记录不存在',
  5001: '用户资料不存在',
  5002: 'API Key 不存在',
  9001: 'AI 服务暂时不可用，请稍后重试',
  9002: '推广执行失败，请稍后重试',
  9003: '加密服务异常',
  9999: '系统内部错误，请稍后重试',
}
```

### 1.4 客户端校验规则

以下校验在调用 API 前由前端执行，避免无效请求：

```typescript
// src/utils/validation.ts
export const VALIDATION_RULES = {
  // 视频上传
  SUPPORTED_VIDEO_FORMATS: ['mp4', 'mov', 'avi', 'mkv'],
  MAX_VIDEO_FILE_SIZE: 5 * 1024 * 1024 * 1024, // 5GB

  // 元数据
  MAX_TITLE_LENGTH: 100,
  MAX_DESCRIPTION_LENGTH: 5000,
  MIN_TAGS: 5,
  MAX_TAGS: 15,

  // 头像
  SUPPORTED_AVATAR_FORMATS: ['jpg', 'jpeg', 'png'],
  MAX_AVATAR_FILE_SIZE: 2 * 1024 * 1024, // 2MB
}

export const VALIDATION_MESSAGES = {
  UNSUPPORTED_VIDEO_FORMAT: '不支持的视频格式，请上传 MP4、MOV、AVI 或 MKV 文件',
  VIDEO_TOO_LARGE: '视频文件超过 5GB 限制',
  TITLE_TOO_LONG: '标题不能超过 100 字符',
  DESCRIPTION_TOO_LONG: '描述不能超过 5000 字符',
  TAGS_TOO_FEW: '至少需要 5 个标签',
  TAGS_TOO_MANY: '标签不能超过 15 个',
  UNSUPPORTED_AVATAR_FORMAT: '仅支持 JPG/PNG 格式',
  AVATAR_TOO_LARGE: '头像文件不能超过 2MB',
}
```

## 2. 类型定义

### 2.1 通用类型 (`src/types/common.ts`)

```typescript
// 统一响应信封
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

// 分页响应
export interface PaginatedData<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

// 分页查询参数
export interface PaginationParams {
  page?: number
  pageSize?: number
  sort?: string
  order?: 'asc' | 'desc'
}
```

### 2.2 领域类型文件清单

| 文件 | 内容 | 关联 API |
|------|------|----------|
| `types/video.ts` | Video, VideoFormat, VideoStatus, UploadSession, UploadProgress, VideoListParams | B1-B6 |
| `types/metadata.ts` | VideoMetadata, MetadataSource, GenerateMetadataRequest, UpdateMetadataRequest | C1-C5 |
| `types/distribution.ts` | PublishRequest, PublishRecord, PublishStatus, DistributionPlatform, PublishProgress | D1-D6 |
| `types/channel.ts` | Channel, ChannelType, ChannelStatus, CreateChannelRequest, UpdateChannelRequest | E1-E5 |
| `types/promotion.ts` | PromotionCopy, PromotionRecord, PromotionMethod, PromotionStatus, ExecutePromotionRequest, PromotionReport | F1-F5 |
| `types/settings.ts` | UserProfile, ConnectedAccount, NotificationPreferences, ApiKey, CreateApiKeyRequest | G1-G10 |
| `types/dashboard.ts` | DashboardOverview, DashboardStats, RecentUpload, PublishDistribution, PromotionOverviewItem, Analytics | A1 |

### 2.3 枚举类型定义示例

```typescript
// types/video.ts
export type VideoFormat = 'MP4' | 'MOV' | 'AVI' | 'MKV'

export type VideoStatus =
  | 'UPLOADED'
  | 'METADATA_GENERATED'
  | 'READY_TO_PUBLISH'
  | 'PUBLISHING'
  | 'PUBLISHED'
  | 'PUBLISH_FAILED'
  | 'PROMOTION_DONE'

export type UploadSessionStatus = 'ACTIVE' | 'COMPLETED' | 'EXPIRED'

export interface Video {
  videoId: string
  fileName: string
  format: VideoFormat
  fileSize: number
  duration: string
  status: VideoStatus
  thumbnailUrl: string | null
  hasMetadata: boolean
  createdAt: string
  updatedAt: string
}

export interface VideoDetail extends Video {
  filePath: string
  metadata: VideoMetadata | null
  publishRecords: PublishRecord[]
}

// types/metadata.ts
export type MetadataSource = 'AI_GENERATED' | 'MANUAL' | 'AI_EDITED'

export interface VideoMetadata {
  metadataId: string
  videoId: string
  title: string
  description: string
  tags: string[]
  source: MetadataSource
  createdAt: string
  updatedAt: string
}

// types/distribution.ts
export type PublishStatus = 'PENDING' | 'UPLOADING' | 'COMPLETED' | 'FAILED' | 'QUOTA_EXCEEDED'

export interface DistributionPlatform {
  platform: string
  displayName: string
  authorized: boolean
  authExpired: boolean
  enabled: boolean
}

// types/channel.ts
export type ChannelType = 'SOCIAL_MEDIA' | 'FORUM' | 'BLOG' | 'OTHER'
export type ChannelStatus = 'ENABLED' | 'DISABLED'

// types/promotion.ts
export type PromotionMethod = 'POST' | 'COMMENT' | 'SHARE'
export type PromotionStatus = 'PENDING' | 'EXECUTING' | 'COMPLETED' | 'FAILED'
```

## 3. React Query Hooks 模式

每个领域一个 hook 文件，统一导出查询和变更 hooks。

### 3.1 模式示例

```typescript
// src/hooks/useVideos.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getVideos, getVideoDetail } from '@/api/video'
import type { VideoListParams } from '@/types/video'

// 查询 key 常量
export const videoKeys = {
  all: ['videos'] as const,
  lists: () => [...videoKeys.all, 'list'] as const,
  list: (params: VideoListParams) => [...videoKeys.lists(), params] as const,
  details: () => [...videoKeys.all, 'detail'] as const,
  detail: (id: string) => [...videoKeys.details(), id] as const,
}

// 视频列表查询
export function useVideoList(params: VideoListParams) {
  return useQuery({
    queryKey: videoKeys.list(params),
    queryFn: () => getVideos(params),
  })
}

// 视频详情查询
export function useVideoDetail(videoId: string) {
  return useQuery({
    queryKey: videoKeys.detail(videoId),
    queryFn: () => getVideoDetail(videoId),
    enabled: !!videoId,
  })
}
```

### 3.2 各领域 Hook 文件清单

| 文件 | 导出 Hook | 说明 |
|------|-----------|------|
| `useDashboard.ts` | `useDashboardOverview(dateRange)` | A1 查询，支持时间范围参数 |
| `useVideos.ts` | `useVideoList(params)`, `useVideoDetail(id)` | B5, B6 查询 |
| `useUpload.ts` | `useInitUpload()`, `useUploadChunk()`, `useCompleteUpload()`, `useUploadProgress(id)` | B1-B4 |
| `useMetadata.ts` | `useVideoMetadata(videoId)`, `useGenerateMetadata()`, `useUpdateMetadata()`, `useRegenerateMetadata()`, `useConfirmMetadata()` | C1-C5 |
| `useDistribution.ts` | `usePlatforms()`, `usePublish()`, `usePublishStatus(taskId)`, `useDistributionRecords(videoId)` | D1-D6 |
| `useChannels.ts` | `useChannelList()`, `useChannelDetail(id)`, `useCreateChannel()`, `useUpdateChannel()`, `useDeleteChannel()` | E1-E5 |
| `usePromotions.ts` | `useGenerateCopy()`, `useExecutePromotion()`, `usePromotionHistory(videoId, params)`, `usePromotionReport(videoId)`, `useRetryPromotion()` | F1-F5 |
| `useSettings.ts` | `useProfile()`, `useUpdateProfile()`, `useUploadAvatar()`, `useConnectedAccounts()`, `useNotifications()`, `useUpdateNotifications()`, `useApiKeys()`, `useCreateApiKey()`, `useDeleteApiKey()` | G1-G10 |

### 3.3 轮询模式

对于需要实时更新的场景（上传进度、发布状态），使用 React Query 的 `refetchInterval`：

```typescript
// 上传进度轮询
export function useUploadProgress(uploadId: string) {
  return useQuery({
    queryKey: ['upload', 'progress', uploadId],
    queryFn: () => getUploadProgress(uploadId),
    enabled: !!uploadId,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'ACTIVE' ? 1000 : false // 进行中时 1 秒轮询
    },
  })
}

// 发布状态轮询
export function usePublishStatus(taskId: string) {
  return useQuery({
    queryKey: ['distribution', 'status', taskId],
    queryFn: () => getPublishStatus(taskId),
    enabled: !!taskId,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'UPLOADING' || status === 'PENDING' ? 2000 : false
    },
  })
}
```

## 4. Zustand Store

单一 store 管理跨页面 UI 状态：

```typescript
// src/store/useAppStore.ts
import { create } from 'zustand'

interface Toast {
  id: string
  type: 'success' | 'error' | 'info'
  message: string
}

interface UploadQueueItem {
  file: File
  uploadId: string | null
  progress: number
  status: 'pending' | 'uploading' | 'completed' | 'failed'
}

interface AppStore {
  // Toast 通知
  toasts: Toast[]
  addToast: (toast: Omit<Toast, 'id'>) => void
  removeToast: (id: string) => void

  // 上传队列
  uploadQueue: UploadQueueItem[]
  addToUploadQueue: (file: File) => void
  updateUploadItem: (index: number, updates: Partial<UploadQueueItem>) => void
  removeFromUploadQueue: (index: number) => void
}
```

## 5. 路由配置

```typescript
// src/App.tsx
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AppLayout } from '@/components/layout/AppLayout'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60,     // 1 分钟
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route element={<AppLayout />}>
            <Route index element={<DashboardPage />} />
            <Route path="videos" element={<VideoManagementPage />} />
            <Route path="upload" element={<VideoUploadPage />} />
            <Route path="videos/:videoId/metadata" element={<MetadataReviewPage />} />
            <Route path="videos/:videoId/distribute" element={<DistributionPromotionPage />} />
            <Route path="promotions" element={<PromotionHistoryPage />} />
            <Route path="settings" element={<SettingsPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
```

### 路由表

| 路径 | 页面组件 | 说明 |
|------|----------|------|
| `/` | `DashboardPage` | 仪表盘首页 |
| `/videos` | `VideoManagementPage` | 视频列表 |
| `/upload` | `VideoUploadPage` | 视频上传 |
| `/videos/:videoId/metadata` | `MetadataReviewPage` | 元数据审核编辑 |
| `/videos/:videoId/distribute` | `DistributionPromotionPage` | 分发与推广向导 |
| `/promotions` | `PromotionHistoryPage` | 推广历史 |
| `/settings` | `SettingsPage` | 用户设置 |

## 6. 布局组件

### 6.1 AppLayout

```
AppLayout
├── Sidebar (fixed, w-[240px], bg-[#001529])
├── Header (fixed, h-16, bg-white/80 backdrop-blur-md)
└── <Outlet /> (main content, ml-[240px] pt-24 px-8 pb-8)
```

AppLayout 使用 React Router 的 `<Outlet />` 渲染子路由页面。

### 6.2 Sidebar

- 固定定位，全屏高度
- Logo 区域：`py-8 px-6`，显示 restaurant_menu 图标 + "Grace" 文字
- 导航项：根据当前路由高亮（使用 `useLocation` 匹配）
- 底部：设置按钮

### 6.3 Header

- 固定定位于 Sidebar 右侧
- 毛玻璃效果：`bg-white/80 backdrop-blur-md`
- 内容：页面标题（左）、用户头像 + 通知铃铛（右）

## 7. 工具函数

### 7.1 格式化 (`src/utils/format.ts`)

```typescript
// 文件大小格式化
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
}

// ISO 8601 Duration 转可读时长（PT12M34S → 12:34）
export function formatDuration(isoDuration: string): string {
  const match = isoDuration.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/)
  if (!match) return '00:00'
  const h = match[1] ? parseInt(match[1]) : 0
  const m = match[2] ? parseInt(match[2]) : 0
  const s = match[3] ? parseInt(match[3]) : 0
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${m}:${String(s).padStart(2, '0')}`
}

// 日期格式化
export function formatDate(isoString: string): string {
  return new Date(isoString).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit'
  })
}

// 百分比格式化
export function formatPercent(value: number): string {
  return `${(value * 100).toFixed(1)}%`
}
```

### 7.2 状态映射 (`src/utils/status.ts`)

```typescript
import type { VideoStatus, PromotionStatus, PublishStatus } from '@/types'

interface StatusConfig {
  label: string
  bgClass: string
  textClass: string
}

export const VIDEO_STATUS_MAP: Record<VideoStatus, StatusConfig> = {
  UPLOADED: { label: '已上传', bgClass: 'bg-surface-container-high', textClass: 'text-on-surface-variant' },
  METADATA_GENERATED: { label: '元数据已生成', bgClass: 'bg-surface-container-high', textClass: 'text-on-surface-variant' },
  READY_TO_PUBLISH: { label: '待发布', bgClass: 'bg-tertiary-fixed', textClass: 'text-on-tertiary-fixed-variant' },
  PUBLISHING: { label: '发布中', bgClass: 'bg-primary-fixed', textClass: 'text-on-primary-fixed' },
  PUBLISHED: { label: '已发布', bgClass: 'bg-secondary-fixed', textClass: 'text-on-secondary-fixed' },
  PUBLISH_FAILED: { label: '发布失败', bgClass: 'bg-error-container', textClass: 'text-on-error-container' },
  PROMOTION_DONE: { label: '推广完成', bgClass: 'bg-secondary-fixed', textClass: 'text-on-secondary-fixed' },
}

export const PROMOTION_STATUS_MAP: Record<PromotionStatus, StatusConfig> = {
  PENDING: { label: '待执行', bgClass: 'bg-surface-container-high', textClass: 'text-on-surface-variant' },
  EXECUTING: { label: '进行中', bgClass: 'bg-secondary-fixed', textClass: 'text-on-secondary-fixed' },
  COMPLETED: { label: '成功', bgClass: 'bg-green-100', textClass: 'text-green-800' },
  FAILED: { label: '失败', bgClass: 'bg-error-container', textClass: 'text-on-error-container' },
}

export const PUBLISH_STATUS_MAP: Record<PublishStatus, StatusConfig> = {
  PENDING: { label: '待处理', bgClass: 'bg-surface-container-high', textClass: 'text-on-surface-variant' },
  UPLOADING: { label: '上传中', bgClass: 'bg-primary-fixed', textClass: 'text-on-primary-fixed' },
  COMPLETED: { label: '发布成功', bgClass: 'bg-green-100', textClass: 'text-green-800' },
  FAILED: { label: '发布失败', bgClass: 'bg-error-container', textClass: 'text-on-error-container' },
  QUOTA_EXCEEDED: { label: '配额超限', bgClass: 'bg-error-container', textClass: 'text-on-error-container' },
}
```
