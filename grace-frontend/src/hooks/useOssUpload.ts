import OSS from 'ali-oss'
import { useCallback, useRef, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { initUpload, getVideoDetail } from '@/api/video'
import { getStsToken } from '@/api/storage'
import type { VideoFormat, UploadSession, VideoStatus } from '@/types/video'

const MAX_RETRY_ATTEMPTS = 3
const POLL_INTERVAL = 2000
const MAX_POLL_ATTEMPTS = 60

export interface OssUploadState {
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

export function useOssUpload() {
  const queryClient = useQueryClient()
  const abortRef = useRef<boolean>(false)
  const startTimeRef = useRef<number>(0)
  const ossClientRef = useRef<OSS | null>(null)
  const uploadSessionRef = useRef<UploadSession | null>(null)

  const [uploadState, setUploadState] = useState<OssUploadState>({
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
  })

  const createOssClient = useCallback((session: UploadSession): OSS => {
    const client = new OSS({
      region: session.stsCredentials.region,
      bucket: session.ossBucket,
      accessKeyId: session.stsCredentials.accessKeyId,
      accessKeySecret: session.stsCredentials.accessKeySecret,
      stsToken: session.stsCredentials.securityToken,
      secure: true,
    })
    return client
  }, [])

  const refreshStsToken = useCallback(async (uploadId: string): Promise<OSS> => {
    const stsResponse = await getStsToken(uploadId)
    const session: UploadSession = {
      uploadId,
      storageKey: stsResponse.storageKey,
      ossBucket: stsResponse.ossBucket,
      stsCredentials: stsResponse.credentials,
      expiresAt: stsResponse.credentials.expiration,
    }
    uploadSessionRef.current = session
    const client = createOssClient(session)
    ossClientRef.current = client
    return client
  }, [createOssClient])

  const uploadWithRetry = useCallback(
    async (
      client: OSS,
      storageKey: string,
      file: File,
      onProgress: (p: number) => void
    ): Promise<void> => {
      let attempts = 0
      while (attempts < MAX_RETRY_ATTEMPTS) {
        if (abortRef.current) {
          throw new Error('Upload cancelled')
        }
        try {
          await client.multipartUpload(storageKey, file, {
            progress: onProgress,
            partSize: 5 * 1024 * 1024,
          })
          return
        } catch (error) {
          attempts++
          if (attempts >= MAX_RETRY_ATTEMPTS) {
            throw error
          }
          const uploadId = uploadSessionRef.current?.uploadId
          if (uploadId) {
            client = await refreshStsToken(uploadId)
          }
          await new Promise((resolve) => setTimeout(resolve, 1000 * attempts))
        }
      }
    },
    [refreshStsToken]
  )

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
      })

      try {
        const format = getVideoFormat(file.name)
        const session = await initUpload({
          fileName: file.name,
          fileSize: file.size,
          format,
        })

        if (abortRef.current) {
          throw new Error('Upload cancelled')
        }

        uploadSessionRef.current = session
        const client = createOssClient(session)
        ossClientRef.current = client

        setUploadState((prev) => ({
          ...prev,
          uploadId: session.uploadId,
          status: 'uploading',
        }))

        await uploadWithRetry(client, session.storageKey, file, (p) => {
          if (abortRef.current) return
          const progress = p * 100
          const elapsedMs = Date.now() - startTimeRef.current
          const uploadedBytes = file.size * p
          const speed = calculateUploadSpeed(uploadedBytes, elapsedMs)
          const remainingBytes = file.size - uploadedBytes
          const estimatedTime = estimateRemainingTime(remainingBytes, speed)
          setUploadState((prev) => ({
            ...prev,
            progress,
            speed,
            estimatedTime,
          }))
        })

        if (abortRef.current) {
          throw new Error('Upload cancelled')
        }

        setUploadState((prev) => ({
          ...prev,
          status: 'completing',
          progress: 100,
        }))

        queryClient.invalidateQueries({ queryKey: ['videos'] })

        const videoId = session.uploadId.replace('upload-', 'video-')
        const videoStatus = await pollVideoStatus(videoId)

        setUploadState((prev) => ({
          ...prev,
          status: 'completed',
          videoId,
          videoStatus,
        }))

        return { videoId, fileName: file.name }
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
    [createOssClient, uploadWithRetry, pollVideoStatus, queryClient]
  )

  const cancelUpload = useCallback(() => {
    abortRef.current = true
    ossClientRef.current = null
    uploadSessionRef.current = null
    setUploadState((prev) => ({
      ...prev,
      status: 'idle',
      uploadId: null,
      progress: 0,
      speed: 0,
      estimatedTime: 0,
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
    })
  }, [])

  return {
    uploadState,
    uploadFile,
    cancelUpload,
    resetState,
  }
}