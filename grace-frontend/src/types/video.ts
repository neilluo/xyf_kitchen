import type { VideoMetadata } from './metadata'
import type { PublishRecord } from './distribution'

// 视频格式
export type VideoFormat = 'MP4' | 'MOV' | 'AVI' | 'MKV'

// 视频状态 - 8种（含软删除 DELETED）
export type VideoStatus =
  | 'UPLOADED'
  | 'METADATA_GENERATED'
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

// 视频详情
export interface VideoDetail extends Video {
  filePath: string
  metadata: VideoMetadata | null
  publishRecords: PublishRecord[]
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
}
