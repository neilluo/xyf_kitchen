import { useCallback, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { serverUploadInit, serverUploadChunk, serverUploadComplete, getVideoDetail } from '@/api/video'
import type { VideoFormat, VideoStatus } from '@/types/video'

const MAX_RETRY_ATTEMPTS = 3
const POLL_INTERVAL = 2000
const MAX_POLL_ATTEMPTS = 60

export interface ServerUploadState {
  uploadId: string | null
  fileName: string
  fileSize: number
  progress: number
  speed: number
  estimatedTime: number
  status: 'idle' | 'initializing' | 'uploading' | 'completing' | 'completed' | 'error'
  error: Error | null
  videoId: string | null
  videoStatus: VideoStatus | null
  totalChunks: number
  uploadedChunks: number
}

function getVideoFormat(fileName: string): VideoFormat {
  const extension = fileName.split('.').pop()?.toUpperCase()
  const validFormats: VideoFormat[] = ['MP4', 'MOV', 'AVI', 'MKV']
  return (validFormats.find((f) => f === extension) ?? 'MP4') as VideoFormat
}

function calculateUploadSpeed(uploadedBytes: number, elapsedMs: number): number {
  if (elapsedMs === 0) return 0
  return (uploadedBytes / elapsedMs) * 1000
}

function estimateRemainingTime(remainingBytes: number, speedBytesPerSecond: number): number {
  if (speedBytesPerSecond === 0) return 0
  return remainingBytes / speedBytesPerSecond
}

export function useServerUpload() {
  const queryClient = useQueryClient()
  const abortRef = useRef<boolean>(false)
  const startTimeRef = useRef<number>(0)

  const [uploadState, setUploadState] = useState<ServerUploadState>({
    uploadId: null,
    fileName: '',
    fileSize: 0,
    progress: 0,
    speed: 0,
    estimatedTime: 0,
    status: 'idle',
    error: null,
    videoId: null,
    videoStatus: null,
    totalChunks: 0,
    uploadedChunks: 0,
  })

  const pollVideoStatus = useCallback(
    async (videoId: string): Promise<VideoStatus> => {
      let attempts = 0
      while (attempts < MAX_POLL_ATTEMPTS && !abortRef.current) {
        try {
          const video = await getVideoDetail(videoId)
          if (video.status !== 'UPLOADED') {
            return video.status
          }
        } catch {
          // Ignore polling errors, retry next interval
        }
        attempts++
        await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL))
      }
      return 'UPLOADED'
    },
    []
  )

  const uploadFile = useCallback(
    async (file: File): Promise<{ videoId: string; fileName: string }> => {
      abortRef.current = false
      startTimeRef.current = Date.now()

      setUploadState({
        uploadId: null,
        fileName: file.name,
        fileSize: file.size,
        progress: 0,
        speed: 0,
        estimatedTime: 0,
        status: 'initializing',
        error: null,
        videoId: null,
        videoStatus: null,
        totalChunks: 0,
        uploadedChunks: 0,
      })

      try {
        const format = getVideoFormat(file.name)
        const session = await serverUploadInit({
          fileName: file.name,
          fileSize: file.size,
          format,
        })

        if (abortRef.current) {
          throw new Error('Upload cancelled')
        }

        setUploadState((prev) => ({
          ...prev,
          uploadId: session.uploadId,
          totalChunks: session.totalChunks,
          uploadedChunks: 0,
          status: 'uploading',
        }))

        for (let i = 0; i < session.totalChunks; i++) {
          if (abortRef.current) {
            throw new Error('Upload cancelled')
          }

          const start = i * session.chunkSize
          const end = Math.min(start + session.chunkSize, file.size)
          const chunk = file.slice(start, end)

          let attempts = 0
          while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
              const result = await serverUploadChunk(session.uploadId, i, chunk)

              const elapsedMs = Date.now() - startTimeRef.current
              const uploadedBytes = (result.uploadedChunks) * session.chunkSize
              const progress = (result.uploadedChunks / session.totalChunks) * 100
              const speed = calculateUploadSpeed(uploadedBytes, elapsedMs)
              const remainingBytes = file.size - uploadedBytes
              const estimatedTime = estimateRemainingTime(remainingBytes, speed)

              setUploadState((prev) => ({
                ...prev,
                uploadedChunks: result.uploadedChunks,
                progress,
                speed,
                estimatedTime,
              }))
              break
            } catch (error) {
              attempts++
              if (attempts >= MAX_RETRY_ATTEMPTS) {
                throw error
              }
              await new Promise((resolve) => setTimeout(resolve, 1000 * attempts))
            }
          }
        }

        if (abortRef.current) {
          throw new Error('Upload cancelled')
        }

        setUploadState((prev) => ({
          ...prev,
          status: 'completing',
          progress: 100,
        }))

        const completeResult = await serverUploadComplete(session.uploadId)

        queryClient.invalidateQueries({ queryKey: ['videos'] })

        const videoStatus = await pollVideoStatus(completeResult.videoId)

        setUploadState((prev) => ({
          ...prev,
          status: 'completed',
          videoId: completeResult.videoId,
          videoStatus,
        }))

        return { videoId: completeResult.videoId, fileName: file.name }
      } catch (error) {
        const err = error instanceof Error ? error : new Error('Upload failed')
        setUploadState((prev) => ({
          ...prev,
          status: 'error',
          error: err,
        }))
        throw err
      }
    },
    [pollVideoStatus, queryClient]
  )

  const cancelUpload = useCallback(() => {
    abortRef.current = true
    setUploadState((prev) => ({
      ...prev,
      status: 'idle',
      uploadId: null,
      progress: 0,
      speed: 0,
      estimatedTime: 0,
      totalChunks: 0,
      uploadedChunks: 0,
    }))
  }, [])

  const resetState = useCallback(() => {
    setUploadState({
      uploadId: null,
      fileName: '',
      fileSize: 0,
      progress: 0,
      speed: 0,
      estimatedTime: 0,
      status: 'idle',
      error: null,
      videoId: null,
      videoStatus: null,
      totalChunks: 0,
      uploadedChunks: 0,
    })
  }, [])

  return {
    uploadState,
    uploadFile,
    cancelUpload,
    resetState,
  }
}