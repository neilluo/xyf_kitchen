import apiClient from './client'
import type { ApiResponse } from '@/types/common'
import type { StsCredentials } from '@/types/video'

interface StsTokenResponse {
  credentials: StsCredentials
  ossBucket: string
  storageKey: string
}

export async function getStsToken(uploadId: string): Promise<StsTokenResponse> {
  const response = await apiClient.post<ApiResponse<StsTokenResponse>>('/storage/oss/sts-token', { uploadId })
  return response.data.data
}

interface OssCallbackResponse {
  videoId: string
  fileName: string
  fileSize: number
  format: string
  duration: string
  status: string
  createdAt: string
}

export async function notifyOssCallback(uploadId: string): Promise<OssCallbackResponse> {
  const response = await apiClient.post<ApiResponse<OssCallbackResponse>>('/storage/oss/callback', { uploadId })
  return response.data.data
}