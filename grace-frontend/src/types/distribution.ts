// 发布状态 - 5种
export type PublishStatus = 'PENDING' | 'UPLOADING' | 'COMPLETED' | 'FAILED' | 'QUOTA_EXCEEDED'

// 分发平台
export interface DistributionPlatform {
  platform: string
  displayName: string
  authorized: boolean
  authExpired: boolean
  enabled: boolean
}

// 发布记录
export interface PublishRecord {
  publishRecordId: string
  platform: string
  status: PublishStatus
  videoUrl: string | null
  progressPercent: number
  errorMessage: string | null
  publishedAt: string | null
  createdAt: string
}

// 发布请求
export interface PublishRequest {
  videoId: string
  metadataId: string
  platform: string
  privacyStatus?: 'public' | 'unlisted' | 'private'
}

// 发布响应
export interface PublishResponse {
  publishRecordId: string
  videoId: string
  platform: string
  uploadTaskId: string
  status: PublishStatus
  createdAt: string
}

// 发布状态查询响应
export interface PublishStatusResponse {
  publishRecordId: string
  taskId: string
  platform: string
  status: PublishStatus
  progressPercent: number
  videoUrl: string | null
  errorMessage: string | null
  publishedAt: string | null
}

// OAuth 授权请求
export interface InitiateAuthRequest {
  redirectUri: string
}

// OAuth 授权响应
export interface AuthUrlResponse {
  authUrl: string
  state: string
}
