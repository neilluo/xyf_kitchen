// 用户资料
export interface UserProfile {
  userId: string
  displayName: string
  email: string | null
  avatarUrl: string | null
  createdAt: string
}

// 更新用户资料请求
export interface UpdateProfileRequest {
  displayName?: string
  email?: string
}

// 已连接账户
export interface ConnectedAccount {
  platform: string
  displayName: string
  authorized: boolean
  accountName: string | null
  connectedAt: string | null
}

// 通知偏好
export interface NotificationPreferences {
  uploadComplete: boolean
  promotionSuccess: boolean
  systemUpdates: boolean
}

// 更新通知偏好请求
export interface UpdateNotificationsRequest {
  uploadComplete?: boolean
  promotionSuccess?: boolean
  systemUpdates?: boolean
}

// API Key
export interface ApiKey {
  apiKeyId: string
  name: string
  prefix: string
  expiresAt: string
  lastUsedAt: string | null
  createdAt: string
}

// API Key 详情（含明文）
export interface ApiKeyWithSecret extends ApiKey {
  key: string
}

// 创建 API Key 请求
export interface CreateApiKeyRequest {
  name: string
  expiresInDays?: number
}

// 头像上传响应
export interface AvatarResponse {
  avatarUrl: string
}
