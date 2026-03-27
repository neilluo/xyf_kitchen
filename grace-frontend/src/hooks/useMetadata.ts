import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  generateMetadata,
  updateMetadata,
  regenerateMetadata,
  confirmMetadata,
  getMetadataByVideoId,
} from '@/api/metadata'
import type { GenerateMetadataRequest, UpdateMetadataRequest } from '@/types/metadata'

// Query key constants
export const metadataKeys = {
  all: ['metadata'] as const,
  lists: () => [...metadataKeys.all, 'list'] as const,
  detail: (id: string) => [...metadataKeys.all, 'detail', id] as const,
  byVideo: (videoId: string) => [...metadataKeys.all, 'video', videoId] as const,
}

// Video metadata query
export function useVideoMetadata(videoId: string) {
  return useQuery({
    queryKey: metadataKeys.byVideo(videoId),
    queryFn: () => getMetadataByVideoId(videoId),
    enabled: !!videoId,
  })
}

// Generate metadata mutation
export function useGenerateMetadata() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: GenerateMetadataRequest) => generateMetadata(request),
    onSuccess: (data) => {
      // Update cache with new metadata
      queryClient.setQueryData(metadataKeys.detail(data.metadataId), data)
      queryClient.setQueryData(metadataKeys.byVideo(data.videoId), data)
      // Invalidate video lists to reflect metadata status change
      queryClient.invalidateQueries({ queryKey: ['videos'] })
    },
  })
}

// Update metadata mutation
export function useUpdateMetadata() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      metadataId,
      request,
    }: {
      metadataId: string
      request: UpdateMetadataRequest
    }) => updateMetadata(metadataId, request),
    onSuccess: (data) => {
      // Update cache with updated metadata
      queryClient.setQueryData(metadataKeys.detail(data.metadataId), data)
      queryClient.setQueryData(metadataKeys.byVideo(data.videoId), data)
    },
  })
}

// Regenerate metadata mutation
export function useRegenerateMetadata() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (metadataId: string) => regenerateMetadata(metadataId),
    onSuccess: (data) => {
      // Update cache with regenerated metadata
      queryClient.setQueryData(metadataKeys.detail(data.metadataId), data)
      queryClient.setQueryData(metadataKeys.byVideo(data.videoId), data)
    },
  })
}

// Confirm metadata mutation
export function useConfirmMetadata() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (metadataId: string) => confirmMetadata(metadataId),
    onSuccess: (data) => {
      // Update cache with confirmed metadata
      queryClient.setQueryData(metadataKeys.detail(data.metadataId), data)
      queryClient.setQueryData(metadataKeys.byVideo(data.videoId), data)
      // Invalidate video lists to reflect status change
      queryClient.invalidateQueries({ queryKey: ['videos'] })
    },
  })
}
