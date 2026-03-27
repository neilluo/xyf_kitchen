// 渠道类型
export type ChannelType = 'SOCIAL_MEDIA' | 'FORUM' | 'BLOG' | 'OTHER'

// 渠道状态
export type ChannelStatus = 'ENABLED' | 'DISABLED'

// 推广渠道
export interface Channel {
  channelId: string
  name: string
  type: ChannelType
  channelUrl: string
  hasApiKey: boolean
  priority: number
  status: ChannelStatus
  createdAt: string
  updatedAt: string
}

// 创建渠道请求
export interface CreateChannelRequest {
  name: string
  type: ChannelType
  channelUrl: string
  apiKey?: string
  priority?: number
}

// 更新渠道请求
export interface UpdateChannelRequest {
  name?: string
  type?: ChannelType
  channelUrl?: string
  apiKey?: string
  priority?: number
  status?: ChannelStatus
}

// 渠道查询参数
export interface ChannelListParams {
  status?: ChannelStatus
  type?: ChannelType
}
