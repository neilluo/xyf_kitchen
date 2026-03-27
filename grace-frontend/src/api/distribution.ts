import apiClient from './client'
import type {
  PublishRequest,
  PublishResponse,
  PublishStatusResponse,
  DistributionPlatform,
  PublishRecord,
  InitiateAuthRequest,
  AuthUrlResponse,
} from '@/types/distribution'
import type { ApiResponse } from '@/types/common'

// D1: POST /api/distribution/publish
export async function publishVideo(request: PublishRequest): Promise<PublishResponse> {
  const response = await apiClient.post<ApiResponse<PublishResponse>>('/distribution/publish', request)
  return response.data.data
}

// D2: GET /api/distribution/status/{taskId}
export async function getPublishStatus(taskId: string): Promise<PublishStatusResponse> {
  const response = await apiClient.get<ApiResponse<PublishStatusResponse>>(
    `/distribution/status/${taskId}`
  )
  return response.data.data
}

// D3: POST /api/distribution/auth/{platform}
export async function initiatePlatformAuth(
  platform: string,
  request: InitiateAuthRequest
): Promise<AuthUrlResponse> {
  const response = await apiClient.post<ApiResponse<AuthUrlResponse>>(
    `/distribution/auth/${platform}`,
    request
  )
  return response.data.data
}

// D4: GET /api/distribution/auth/{platform}/callback
// Note: This is handled by backend redirect, frontend typically calls this after OAuth redirect
export async function handleAuthCallback(
  platform: string,
  code: string,
  state: string
): Promise<void> {
  await apiClient.get(`/distribution/auth/${platform}/callback`, {
    params: { code, state },
  })
}

// D5: GET /api/distribution/platforms
export async function getPlatforms(): Promise<DistributionPlatform[]> {
  const response = await apiClient.get<ApiResponse<DistributionPlatform[]>>('/distribution/platforms')
  return response.data.data
}

// D6: GET /api/distribution/records/{videoId}
export async function getPublishRecords(videoId: string): Promise<PublishRecord[]> {
  const response = await apiClient.get<ApiResponse<PublishRecord[]>>(
    `/distribution/records/${videoId}`
  )
  return response.data.data
}
