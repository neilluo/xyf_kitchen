import apiClient from './client'
import type {
  UserProfile,
  UpdateProfileRequest,
  ConnectedAccount,
  NotificationPreferences,
  UpdateNotificationsRequest,
  ApiKey,
  ApiKeyWithSecret,
  CreateApiKeyRequest,
  AvatarResponse,
} from '@/types/settings'
import type { ApiResponse } from '@/types/common'

// G1: GET /api/settings/profile
export async function getProfile(): Promise<UserProfile> {
  const response = await apiClient.get<ApiResponse<UserProfile>>('/settings/profile')
  return response.data.data
}

// G2: PUT /api/settings/profile
export async function updateProfile(request: UpdateProfileRequest): Promise<UserProfile> {
  const response = await apiClient.put<ApiResponse<UserProfile>>('/settings/profile', request)
  return response.data.data
}

// G3: POST /api/settings/profile/avatar
export async function uploadAvatar(avatar: File): Promise<AvatarResponse> {
  const formData = new FormData()
  formData.append('avatar', avatar)

  const response = await apiClient.post<ApiResponse<AvatarResponse>>(
    '/settings/profile/avatar',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  )
  return response.data.data
}

// G4: GET /api/settings/connected-accounts
export async function getConnectedAccounts(): Promise<ConnectedAccount[]> {
  const response = await apiClient.get<ApiResponse<ConnectedAccount[]>>(
    '/settings/connected-accounts'
  )
  return response.data.data
}

// G5: DELETE /api/settings/connected-accounts/{platform}
export async function disconnectPlatform(platform: string): Promise<void> {
  await apiClient.delete(`/settings/connected-accounts/${platform}`)
}

// G6: GET /api/settings/notifications
export async function getNotifications(): Promise<NotificationPreferences> {
  const response = await apiClient.get<ApiResponse<NotificationPreferences>>(
    '/settings/notifications'
  )
  return response.data.data
}

// G7: PUT /api/settings/notifications
export async function updateNotifications(
  request: UpdateNotificationsRequest
): Promise<NotificationPreferences> {
  const response = await apiClient.put<ApiResponse<NotificationPreferences>>(
    '/settings/notifications',
    request
  )
  return response.data.data
}

// G8: POST /api/settings/api-keys
export async function createApiKey(request: CreateApiKeyRequest): Promise<ApiKeyWithSecret> {
  const response = await apiClient.post<ApiResponse<ApiKeyWithSecret>>(
    '/settings/api-keys',
    request
  )
  return response.data.data
}

// G9: GET /api/settings/api-keys
export async function getApiKeys(): Promise<ApiKey[]> {
  const response = await apiClient.get<ApiResponse<ApiKey[]>>('/settings/api-keys')
  return response.data.data
}

// G10: DELETE /api/settings/api-keys/{apiKeyId}
export async function deleteApiKey(apiKeyId: string): Promise<void> {
  await apiClient.delete(`/settings/api-keys/${apiKeyId}`)
}
