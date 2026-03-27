import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createChannel,
  updateChannel,
  deleteChannel,
  getChannels,
  getChannelDetail,
} from '@/api/channel'
import type { CreateChannelRequest, UpdateChannelRequest, ChannelListParams } from '@/types/channel'

// Query key constants
export const channelKeys = {
  all: ['channels'] as const,
  lists: () => [...channelKeys.all, 'list'] as const,
  list: (params?: ChannelListParams) => [...channelKeys.lists(), params] as const,
  details: () => [...channelKeys.all, 'detail'] as const,
  detail: (id: string) => [...channelKeys.details(), id] as const,
}

// Channel list query
export function useChannelList(params?: ChannelListParams) {
  return useQuery({
    queryKey: channelKeys.list(params),
    queryFn: () => getChannels(params),
  })
}

// Channel detail query
export function useChannelDetail(channelId: string) {
  return useQuery({
    queryKey: channelKeys.detail(channelId),
    queryFn: () => getChannelDetail(channelId),
    enabled: !!channelId,
  })
}

// Create channel mutation
export function useCreateChannel() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: CreateChannelRequest) => createChannel(request),
    onSuccess: () => {
      // Invalidate channel lists
      queryClient.invalidateQueries({ queryKey: channelKeys.lists() })
    },
  })
}

// Update channel mutation
export function useUpdateChannel() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      channelId,
      request,
    }: {
      channelId: string
      request: UpdateChannelRequest
    }) => updateChannel(channelId, request),
    onSuccess: (data) => {
      // Update cache with updated channel
      queryClient.setQueryData(channelKeys.detail(data.channelId), data)
      // Invalidate channel lists
      queryClient.invalidateQueries({ queryKey: channelKeys.lists() })
    },
  })
}

// Delete channel mutation
export function useDeleteChannel() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (channelId: string) => deleteChannel(channelId),
    onSuccess: (_, channelId) => {
      // Remove from cache
      queryClient.removeQueries({ queryKey: channelKeys.detail(channelId) })
      // Invalidate channel lists
      queryClient.invalidateQueries({ queryKey: channelKeys.lists() })
    },
  })
}
