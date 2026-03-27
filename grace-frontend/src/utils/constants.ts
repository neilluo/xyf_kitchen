// 路由路径常量
export const ROUTES = {
  // 仪表盘
  DASHBOARD: '/',

  // 视频管理
  VIDEOS: '/videos',
  UPLOAD: '/upload',
  VIDEO_METADATA: (videoId: string) => `/videos/${videoId}/metadata`,
  VIDEO_DISTRIBUTE: (videoId: string) => `/videos/${videoId}/distribute`,

  // 推广历史
  PROMOTIONS: '/promotions',

  // 设置
  SETTINGS: '/settings',
} as const

// API 端点路径常量
export const API_ENDPOINTS = {
  // 仪表盘
  DASHBOARD_OVERVIEW: '/dashboard/overview',

  // 视频
  VIDEOS: '/videos',
  UPLOAD_INIT: '/videos/upload/init',
  UPLOAD_CHUNK: '/videos/upload/chunk',
  UPLOAD_COMPLETE: '/videos/upload/complete',
  UPLOAD_PROGRESS: (uploadId: string) => `/videos/upload/${uploadId}/progress`,
  VIDEO_DETAIL: (videoId: string) => `/videos/${videoId}`,

  // 元数据
  METADATA_GENERATE: (videoId: string) => `/videos/${videoId}/metadata/generate`,
  METADATA_UPDATE: (metadataId: string) => `/metadata/${metadataId}`,
  METADATA_REGENERATE: (metadataId: string) => `/metadata/${metadataId}/regenerate`,
  METADATA_CONFIRM: (metadataId: string) => `/metadata/${metadataId}/confirm`,
  METADATA_BY_VIDEO: (videoId: string) => `/videos/${videoId}/metadata`,

  // 分发
  PLATFORMS: '/distribution/platforms',
  PUBLISH: '/distribution/publish',
  PUBLISH_STATUS: (taskId: string) => `/distribution/publish/${taskId}/status`,
  PLATFORM_AUTH: (platform: string) => `/distribution/platforms/${platform}/auth`,
  PLATFORM_AUTH_CALLBACK: (platform: string) => `/distribution/platforms/${platform}/auth/callback`,
  PUBLISH_RECORDS: (videoId: string) => `/videos/${videoId}/distribution/records`,

  // 渠道
  CHANNELS: '/channels',
  CHANNEL_DETAIL: (channelId: string) => `/channels/${channelId}`,

  // 推广
  PROMOTION_COPY: (videoId: string) => `/videos/${videoId}/promotion/copy`,
  PROMOTION_EXECUTE: (videoId: string) => `/videos/${videoId}/promotion/execute`,
  PROMOTION_HISTORY: (videoId: string) => `/videos/${videoId}/promotion/history`,
  PROMOTION_REPORT: (videoId: string) => `/videos/${videoId}/promotion/report`,
  PROMOTION_RETRY: (recordId: string) => `/promotion/${recordId}/retry`,

  // 设置
  PROFILE: '/settings/profile',
  AVATAR: '/settings/avatar',
  CONNECTED_ACCOUNTS: '/settings/connected-accounts',
  NOTIFICATIONS: '/settings/notifications',
  API_KEYS: '/settings/api-keys',
} as const
