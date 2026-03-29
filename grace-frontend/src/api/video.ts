import apiClient from './client'
import type {
  Video,
  VideoDetail,
  VideoListParams,
  InitUploadRequest,
  UploadSession,
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