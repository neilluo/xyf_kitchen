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

// STS 临时凭证
export interface StsCredentials {
  accessKeyId: string
  accessKeySecret: string
  securityToken: string
  region: string
  expiration: string
}

// 上传会话（OSS 直传）
export interface UploadSession {
  uploadId: string
  storageKey: string
  ossBucket: string
  stsCredentials: StsCredentials
  expiresAt: string
}

export interface InitUploadRequest {
  fileName: string
  fileSize: number
  format: VideoFormat
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

export type UploadMode = 'DIRECT_OSS' | 'SERVER_UPLOAD'

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
