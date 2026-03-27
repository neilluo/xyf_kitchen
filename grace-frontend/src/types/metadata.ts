// 元数据来源
export type MetadataSource = 'AI_GENERATED' | 'MANUAL' | 'AI_EDITED'

// 视频元数据
export interface VideoMetadata {
  metadataId: string
  videoId: string
  title: string
  description: string
  tags: string[]
  source: MetadataSource
  createdAt: string
  updatedAt: string
}

// 生成元数据请求
export interface GenerateMetadataRequest {
  videoId: string
}

// 更新元数据请求
export interface UpdateMetadataRequest {
  title?: string
  description?: string
  tags?: string[]
}
