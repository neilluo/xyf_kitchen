import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { initUpload, uploadChunk, completeUpload, getUploadProgress } from '@/api/video'
import type { InitUploadRequest, VideoFormat } from '@/types/video'

// Query key constants
export const uploadKeys = {
  all: ['upload'] as const,
  progress: (uploadId: string) => [...uploadKeys.all, 'progress', uploadId] as const,
}

// Constants
const CHUNK_SIZE = 5 * 1024 * 1024 // 5MB chunks

// Initialize upload mutation
export function useInitUpload() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: InitUploadRequest) => initUpload(request),
    onSuccess: () => {
      // Invalidate video lists after successful upload
      queryClient.invalidateQueries({ queryKey: ['videos'] })
    },
  })
}

// Upload chunk mutation
export function useUploadChunk() {
  return useMutation({
    mutationFn: ({
      uploadId,
      chunkIndex,
      chunk,
    }: {
      uploadId: string
      chunkIndex: number
      chunk: Blob
    }) => uploadChunk(uploadId, chunkIndex, chunk),
  })
}

// Complete upload mutation
export function useCompleteUpload() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (uploadId: string) => completeUpload(uploadId),
    onSuccess: () => {
      // Invalidate video lists after upload completion
      queryClient.invalidateQueries({ queryKey: ['videos'] })
    },
  })
}

// Upload progress query with polling
export function useUploadProgress(uploadId: string) {
  return useQuery({
    queryKey: uploadKeys.progress(uploadId),
    queryFn: () => getUploadProgress(uploadId),
    enabled: !!uploadId,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'ACTIVE' ? 1000 : false // Poll every 1 second while active
    },
  })
}

// Helper function to calculate upload speed
export function calculateUploadSpeed(
  uploadedBytes: number,
  elapsedMs: number
): number {
  if (elapsedMs === 0) return 0
  return (uploadedBytes / elapsedMs) * 1000 // bytes per second
}

// Helper function to estimate remaining time
export function estimateRemainingTime(
  remainingBytes: number,
  speedBytesPerSecond: number
): number {
  if (speedBytesPerSecond === 0) return 0
  return remainingBytes / speedBytesPerSecond // seconds
}

// Helper function to split file into chunks
export function createChunks(file: File, chunkSize: number = CHUNK_SIZE): Blob[] {
  const chunks: Blob[] = []
  let offset = 0

  while (offset < file.size) {
    const chunk = file.slice(offset, offset + chunkSize)
    chunks.push(chunk)
    offset += chunkSize
  }

  return chunks
}

// Helper function to get video format from file name
export function getVideoFormat(fileName: string): VideoFormat {
  const extension = fileName.split('.').pop()?.toUpperCase()
  const validFormats: VideoFormat[] = ['MP4', 'MOV', 'AVI', 'MKV']
  return (validFormats.find((f) => f === extension) ?? 'MP4') as VideoFormat
}

// Upload state for tracking progress
export interface UploadState {
  uploadId: string | null
  fileName: string
  fileSize: number
  totalChunks: number
  uploadedChunks: number
  progressPercent: number
  uploadSpeed: number // bytes per second
  estimatedTimeRemaining: number // seconds
  status: 'idle' | 'initializing' | 'uploading' | 'completing' | 'completed' | 'error'
  error: Error | null
}
