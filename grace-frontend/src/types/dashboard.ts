// 仪表盘概览
export interface DashboardOverview {
  stats: DashboardStats
  recentUploads: RecentUpload[]
  publishDistribution: PublishDistribution
  promotionOverview: PromotionOverviewItem[]
  analytics: Analytics
}

// 统计卡片数据
export interface DashboardStats {
  totalVideos: number
  pendingReview: number
  published: number
  promoting: number
}

// 最近上传
export interface RecentUpload {
  videoId: string
  fileName: string
  thumbnailUrl: string | null
  status: string
  createdAt: string
}

// 发布状态分布
export interface PublishDistribution {
  published: number
  pending: number
  failed: number
}

// 推广概览项
export interface PromotionOverviewItem {
  channelId: string
  channelName: string
  totalExecutions: number
  successCount: number
  failedCount: number
  successRate: number
}

// 分析数据
export interface Analytics {
  avgEngagementRate: number
  totalImpressions: number
}

// 时间范围
export type DateRange = '7d' | '30d' | '90d' | 'all'

// 仪表盘查询参数
export interface DashboardParams {
  dateRange?: DateRange
}
