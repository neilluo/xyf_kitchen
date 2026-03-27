import apiClient from './client'
import type {
  Channel,
  CreateChannelRequest,
  UpdateChannelRequest,
  ChannelListParams,
} from '@/types/channel'
import type { ApiResponse } from '@/types/common'

// E1: POST /api/channels
export async function createChannel(request: CreateChannelRequest): Promise<Channel> {
  const response = await apiClient.post<ApiResponse<Channel>>('/channels', request)
  return response.data.data
}

// E2: PUT /api/channels/{id}
export async function updateChannel(
  channelId: string,
  request: UpdateChannelRequest
): Promise<Channel> {
  const response = await apiClient.put<ApiResponse<Channel>>(`/channels/${channelId}`, request)
  return response.data.data
}

// E3: DELETE /api/channels/{id}
export async function deleteChannel(channelId: string): Promise<void> {
  await apiClient.delete(`/channels/${channelId}`)
}

// E4: GET /api/channels
export async function getChannels(params?: ChannelListParams): Promise<Channel[]> {
  const response = await apiClient.get<ApiResponse<Channel[]>>('/channels', {
    params: {
      status: params?.status,
      type: params?.type,
    },
  })
  return response.data.data
}

// E5: GET /api/channels/{id}
export async function getChannelDetail(channelId: string): Promise<Channel> {
  const response = await apiClient.get<ApiResponse<Channel>>(`/channels/${channelId}`)
  return response.data.data
}
