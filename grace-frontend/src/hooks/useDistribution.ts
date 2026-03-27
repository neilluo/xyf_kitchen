import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  publishVideo,
  getPublishStatus,
  getPlatforms,
  getPublishRecords,
} from '@/api/distribution'
import type { PublishRequest } from '@/types/distribution'

// Query key constants
export const distributionKeys = {
  all: ['distribution'] as const,
  platforms: () => [...distributionKeys.all, 'platforms'] as const,
  status: (taskId: string) => [...distributionKeys.all, 'status', taskId] as const,
  records: (videoId: string) => [...distributionKeys.all, 'records', videoId] as const,
}

// Platforms query
export function usePlatforms() {
  return useQuery({
    queryKey: distributionKeys.platforms(),
    queryFn: () => getPlatforms(),
  })
}

// Publish video mutation
export function usePublish() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: PublishRequest) => publishVideo(request),
    onSuccess: (data) => {
      // Invalidate related queries
      queryClient.invalidateQueries({ queryKey: distributionKeys.records(data.videoId) })
      queryClient.invalidateQueries({ queryKey: ['videos'] })
    },
  })
}

// Publish status query with polling
export function usePublishStatus(taskId: string) {
  return useQuery({
    queryKey: distributionKeys.status(taskId),
    queryFn: () => getPublishStatus(taskId),
    enabled: !!taskId,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'UPLOADING' || status === 'PENDING' ? 2000 : false // Poll every 2 seconds
    },
  })
}

// Distribution records query
export function useDistributionRecords(videoId: string) {
  return useQuery({
    queryKey: distributionKeys.records(videoId),
    queryFn: () => getPublishRecords(videoId),
    enabled: !!videoId,
  })
}
