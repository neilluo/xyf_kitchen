import apiClient from './client'
import type {
  Video,
  VideoDetail,
  VideoListParams,
  InitUploadRequest,
  UploadSession,
  ServerUploadSession,
  ServerUploadInitRequest,
  ServerChunkUploadResponse,
  ServerUploadCompleteResponse,
} from '@/types/video'
import type { PaginatedData, ApiResponse } from '@/types/common'

export async function initUpload(request: InitUploadRequest): Promise<UploadSession> {
  const response = await apiClient.post<ApiResponse<UploadSession>>('/videos/upload/init', request)
  return response.data.data
}

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

export async function getVideoDetail(videoId: string): Promise<VideoDetail> {
  const response = await apiClient.get<ApiResponse<VideoDetail>>(`/videos/${videoId}`)
  return response.data.data
}

export async function serverUploadInit(request: ServerUploadInitRequest): Promise<ServerUploadSession> {
  const response = await apiClient.post<ApiResponse<ServerUploadSession>>('/videos/upload/server/init', request)
  return response.data.data
}

export async function serverUploadChunk(
  uploadId: string,
  chunkIndex: number,
  chunk: Blob
): Promise<ServerChunkUploadResponse> {
  const formData = new FormData()
  formData.append('chunkIndex', String(chunkIndex))
  formData.append('chunk', chunk)
  const response = await apiClient.post<ApiResponse<ServerChunkUploadResponse>>(
    `/videos/upload/server/${uploadId}/chunk`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  )
  return response.data.data
}

export async function serverUploadComplete(uploadId: string): Promise<ServerUploadCompleteResponse> {
  const response = await apiClient.post<ApiResponse<ServerUploadCompleteResponse>>(
    `/videos/upload/server/${uploadId}/complete`
  )
  return response.data.data
}