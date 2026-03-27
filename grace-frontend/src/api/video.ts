import apiClient from './client'
import type {
  Video,
  VideoDetail,
  VideoListParams,
  InitUploadRequest,
  UploadSession,
  UploadChunkResponse,
  UploadProgress,
  CompleteUploadResponse,
} from '@/types/video'
import type { PaginatedData, ApiResponse } from '@/types/common'

// B1: POST /api/videos/upload/init
export async function initUpload(request: InitUploadRequest): Promise<UploadSession> {
  const response = await apiClient.post<ApiResponse<UploadSession>>('/videos/upload/init', request)
  return response.data.data
}

// B2: POST /api/videos/upload/{uploadId}/chunk
export async function uploadChunk(
  uploadId: string,
  chunkIndex: number,
  chunk: Blob
): Promise<UploadChunkResponse> {
  const formData = new FormData()
  formData.append('chunkIndex', chunkIndex.toString())
  formData.append('chunk', chunk)

  const response = await apiClient.post<ApiResponse<UploadChunkResponse>>(
    `/videos/upload/${uploadId}/chunk`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  )
  return response.data.data
}

// B3: POST /api/videos/upload/{uploadId}/complete
export async function completeUpload(uploadId: string): Promise<CompleteUploadResponse> {
  const response = await apiClient.post<ApiResponse<CompleteUploadResponse>>(
    `/videos/upload/${uploadId}/complete`
  )
  return response.data.data
}

// B4: GET /api/videos/upload/{uploadId}/progress
export async function getUploadProgress(uploadId: string): Promise<UploadProgress> {
  const response = await apiClient.get<ApiResponse<UploadProgress>>(
    `/videos/upload/${uploadId}/progress`
  )
  return response.data.data
}

// B5: GET /api/videos
export async function getVideos(params: VideoListParams): Promise<PaginatedData<Video>> {
  const response = await apiClient.get<ApiResponse<PaginatedData<Video>>>('/videos', {
    params: {
      page: params.page ?? 1,
      pageSize: params.pageSize ?? 20,
      keyword: params.keyword,
      status: params.status,
      startDate: params.startDate,
      endDate: params.endDate,
      sort: params.sort ?? 'createdAt',
      order: params.order ?? 'desc',
    },
  })
  return response.data.data
}

// B6: GET /api/videos/{videoId}
export async function getVideoDetail(videoId: string): Promise<VideoDetail> {
  const response = await apiClient.get<ApiResponse<VideoDetail>>(`/videos/${videoId}`)
  return response.data.data
}
