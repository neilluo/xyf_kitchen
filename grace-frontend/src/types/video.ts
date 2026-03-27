import type { VideoMetadata } from './metadata'
import type { PublishRecord } from './distribution'

// 视频格式
export type VideoFormat = 'MP4' | 'MOV' | 'AVI' | 'MKV'

// 视频状态 - 7种
export type VideoStatus =
  | 'UPLOADED'
  | 'METADATA_GENERATED'
  | 'READY_TO_PUBLISH'
  | 'PUBLISHING'
  | 'PUBLISHED'
  | 'PUBLISH_FAILED'
  | 'PROMOTION_DONE'

// 上传会话状态
export type UploadSessionStatus = 'ACTIVE' | 'COMPLETED' | 'EXPIRED'

// 视频列表
export interface Video {
  videoId: string
  fileName: string
  format: VideoFormat
  fileSize: number
  duration: string
  status: VideoStatus
  thumbnailUrl: string | null
  hasMetadata: boolean
  createdAt: string
  updatedAt: string
}

// 视频详情
export interface VideoDetail extends Video {
  filePath: string
  metadata: VideoMetadata | null
  publishRecords: PublishRecord[]
}

// 上传会话
export interface UploadSession {
  uploadId: string
  totalChunks: number
  chunkSize: number
  expiresAt: string
}

// 上传进度
export interface UploadProgress {
  uploadId: string
  uploadedChunks: number
  totalChunks: number
  progressPercent: number
  status: UploadSessionStatus
}

// 初始化上传请求
export interface InitUploadRequest {
  fileName: string
  fileSize: number
  format: VideoFormat
}

// 上传分片响应
export interface UploadChunkResponse {
  uploadId: string
  chunkIndex: number
  uploadedChunks: number
  totalChunks: number
}

// 完成上传响应
export interface CompleteUploadResponse {
  videoId: string
  fileName: string
  fileSize: number
  format: VideoFormat
  duration: string
  status: VideoStatus
  createdAt: string
}

// 视频列表查询参数
export interface VideoListParams {
  page?: number
  pageSize?: number
  keyword?: string
  status?: string
  startDate?: string
  endDate?: string
  sort?: string
  order?: 'asc' | 'desc'
}
