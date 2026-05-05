import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getVideos, getVideoDetail, deleteVideo } from '@/api/video'
import type { VideoListParams } from '@/types/video'

// Query key constants
export const videoKeys = {
  all: ['videos'] as const,
  lists: () => [...videoKeys.all, 'list'] as const,
  list: (params: VideoListParams) => [...videoKeys.lists(), params] as const,
  details: () => [...videoKeys.all, 'detail'] as const,
  detail: (id: string) => [...videoKeys.details(), id] as const,
}

// Video list query
export function useVideoList(params: VideoListParams) {
  return useQuery({
    queryKey: videoKeys.list(params),
    queryFn: () => getVideos(params),
  })
}

// Video detail query
export function useVideoDetail(videoId: string) {
  return useQuery({
    queryKey: videoKeys.detail(videoId),
    queryFn: () => getVideoDetail(videoId),
    enabled: !!videoId,
  })
}

// Delete video mutation (软删除)
export function useDeleteVideo() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (videoId: string) => deleteVideo(videoId),
    onSuccess: (_, videoId) => {
      queryClient.removeQueries({ queryKey: videoKeys.detail(videoId) })
      queryClient.invalidateQueries({ queryKey: videoKeys.lists() })
    },
  })
}
