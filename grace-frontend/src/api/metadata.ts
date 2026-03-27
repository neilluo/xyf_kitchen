import apiClient from './client'
import type {
  VideoMetadata,
  GenerateMetadataRequest,
  UpdateMetadataRequest,
} from '@/types/metadata'
import type { ApiResponse } from '@/types/common'

// C1: POST /api/metadata/generate
export async function generateMetadata(
  request: GenerateMetadataRequest
): Promise<VideoMetadata> {
  const response = await apiClient.post<ApiResponse<VideoMetadata>>('/metadata/generate', request)
  return response.data.data
}

// C2: PUT /api/metadata/{id}
export async function updateMetadata(
  metadataId: string,
  request: UpdateMetadataRequest
): Promise<VideoMetadata> {
  const response = await apiClient.put<ApiResponse<VideoMetadata>>(`/metadata/${metadataId}`, request)
  return response.data.data
}

// C3: POST /api/metadata/{id}/regenerate
export async function regenerateMetadata(metadataId: string): Promise<VideoMetadata> {
  const response = await apiClient.post<ApiResponse<VideoMetadata>>(
    `/metadata/${metadataId}/regenerate`
  )
  return response.data.data
}

// C4: POST /api/metadata/{id}/confirm
export async function confirmMetadata(metadataId: string): Promise<VideoMetadata> {
  const response = await apiClient.post<ApiResponse<VideoMetadata>>(
    `/metadata/${metadataId}/confirm`
  )
  return response.data.data
}

// C5: GET /api/metadata/video/{videoId}
export async function getMetadataByVideoId(videoId: string): Promise<VideoMetadata | null> {
  const response = await apiClient.get<ApiResponse<VideoMetadata | null>>(
    `/metadata/video/${videoId}`
  )
  return response.data.data
}
