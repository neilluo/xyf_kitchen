import type { VideoMetadata } from './metadata'
import type { PublishRecord } from './distribution'

// 视频格式
export type VideoFormat = 'MP4' | 'MOV' | 'AVI' | 'MKV'

// 视频状态 - 9种（含软删除 DELETED、元数据失败 METADATA_FAILED）
export type VideoStatus =
  | 'UPLOADED'
  | 'METADATA_GENERATED'
  | 'METADATA_FAILED'
  | 'READY_TO_PUBLISH'
  | 'PUBLISHING'
  | 'PUBLISHED'
  | 'PUBLISH_FAILED'
  | 'PROMOTION_DONE'
  | 'DELETED'

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

// 异步任务摘要
export interface TaskSummary {
  currentStep: string
  status: 'PENDING' | 'RUNNING' | 'FAILED' | 'DONE'
  attempts: number
  lastError: string | null
}

// 视频详情
export interface VideoDetail extends Video {
  filePath: string
  metadata: VideoMetadata | null
  publishRecords: PublishRecord[]
  task?: TaskSummary
}

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

export interface ServerUploadSession {
  uploadId: string
  totalChunks: number
  chunkSize: number
  expiresAt: string
}

export interface ServerUploadInitRequest {
  fileName: string
  fileSize: number
  format: VideoFormat
}

export interface ServerChunkUploadRequest {
  uploadId: string
  chunkIndex: number
  chunk: Blob
}

export interface ServerChunkUploadResponse {
  uploadedChunks: number
  totalChunks: number
}

export interface ServerUploadCompleteResponse {
  videoId: string
  fileName: string
  status: VideoStatus
  taskId: string
}
