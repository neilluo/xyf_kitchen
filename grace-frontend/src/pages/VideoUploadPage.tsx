import { useState, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Icon } from '@/components/ui/Icon'
import { ProgressBar } from '@/components/ui/ProgressBar'
import { useInitUpload, useUploadChunk, useCompleteUpload, calculateUploadSpeed, estimateRemainingTime, getVideoFormat } from '@/hooks/useUpload'
import { useAppStore } from '@/store/useAppStore'
import { formatFileSize } from '@/utils/format'
import { APP_ROUTES } from '@/utils/constants'

// Supported video formats
const SUPPORTED_FORMATS = ['mp4', 'mov', 'avi', 'mkv']
const MAX_FILE_SIZE = 5 * 1024 * 1024 * 1024 // 5GB in bytes
const MAX_RETRY_ATTEMPTS = 3

interface ValidationError {
  message: string
  code: 'FORMAT' | 'SIZE'
}

function validateFile(file: File): ValidationError | null {
  const ext = file.name.split('.').pop()?.toLowerCase()

  if (!ext || !SUPPORTED_FORMATS.includes(ext)) {
    return {
      message: `不支持的视频格式，请上传 ${SUPPORTED_FORMATS.join('、').toUpperCase()} 文件`,
      code: 'FORMAT',
    }
  }

  if (file.size > MAX_FILE_SIZE) {
    return {
      message: '文件大小超过 5GB 限制',
      code: 'SIZE',
    }
  }

  return null
}

// Uploading file state
interface UploadingFile {
  file: File
  uploadId: string | null
  progress: number
  uploadedChunks: number
  totalChunks: number
  speed: number // bytes per second
  estimatedTime: number // seconds
  status: 'initializing' | 'uploading' | 'completing' | 'completed' | 'error'
  error: string | null
}

// Completed upload state
interface CompletedUpload {
  videoId: string
  fileName: string
  fileSize: number
  completedAt: Date
}

interface DropZoneProps {
  onFileSelect: (file: File) => void
  onValidationError: (error: ValidationError) => void
  disabled?: boolean
}

function DropZone({ onFileSelect, onValidationError, disabled }: DropZoneProps) {
  const [isDragging, setIsDragging] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleDragEnter = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    if (!disabled) setIsDragging(true)
  }, [disabled])

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
  }, [])

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
  }, [])

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      e.stopPropagation()
      setIsDragging(false)

      if (disabled) return

      const files = e.dataTransfer.files
      if (files.length > 0) {
        const file = files[0]
        const error = validateFile(file)
        if (error) {
          onValidationError(error)
        } else {
          onFileSelect(file)
        }
      }
    },
    [onFileSelect, onValidationError, disabled]
  )

  const handleClick = useCallback(() => {
    if (!disabled) fileInputRef.current?.click()
  }, [disabled])

  const handleFileInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const files = e.target.files
      if (files && files.length > 0) {
        const file = files[0]
        const error = validateFile(file)
        if (error) {
          onValidationError(error)
        } else {
          onFileSelect(file)
        }
      }
      e.target.value = ''
    },
    [onFileSelect, onValidationError]
  )

  return (
    <div
      className={`relative rounded-xl p-12 text-center cursor-pointer overflow-hidden ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
      style={{
        backgroundColor: isDragging ? 'rgba(0, 87, 194, 0.1)' : 'rgba(240, 245, 255, 0.5)',
      }}
      onDragEnter={handleDragEnter}
      onDragLeave={handleDragLeave}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
      onClick={handleClick}
    >
      <svg
        className="absolute inset-0 w-full h-full pointer-events-none"
        style={{ borderRadius: '0.75rem' }}
      >
        <rect
          rx="12"
          ry="12"
          stroke="#0057c2"
          strokeOpacity={isDragging ? 0.6 : 0.3}
          strokeWidth="2"
          strokeDasharray="8 4"
          fill="none"
          width="100%"
          height="100%"
        />
      </svg>

      <div className="relative z-10 flex flex-col items-center">
        <div className="w-20 h-20 bg-primary-fixed rounded-full flex items-center justify-center mb-6">
          <Icon
            name="cloud_upload"
            size={48}
            className="text-primary"
            style={{ fontVariationSettings: "'FILL' 1" }}
          />
        </div>

        <h2 className="font-headline text-lg font-bold text-on-surface">
          拖拽视频文件到此处
        </h2>

        <p className="font-body text-sm text-on-surface-variant mt-2">
          或点击选择文件
        </p>

        <p className="font-body text-xs text-on-surface-variant/60 mt-4">
          支持 MP4、MOV、AVI、MKV，最大 5GB
        </p>
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept=".mp4,.mov,.avi,.mkv,video/mp4,video/quicktime,video/x-msvideo,video/x-matroska"
        className="hidden"
        onChange={handleFileInputChange}
        disabled={disabled}
      />
    </div>
  )
}

interface ValidationErrorToastProps {
  error: ValidationError | null
  onClose: () => void
}

function ValidationErrorToast({ error, onClose }: ValidationErrorToastProps) {
  if (!error) return null

  return (
    <div className="fixed bottom-6 right-6 z-50 animate-slide-up">
      <div className="bg-error-container text-on-error-container px-6 py-4 rounded-lg shadow-lg flex items-center gap-3">
        <Icon name="error" size={20} className="text-error" />
        <span className="font-body text-sm">{error.message}</span>
        <button
          onClick={onClose}
          className="ml-2 p-1 hover:bg-error/10 rounded transition-colors"
        >
          <Icon name="close" size={16} />
        </button>
      </div>
    </div>
  )
}

interface UploadProgressCardProps {
  upload: UploadingFile
  onCancel: () => void
}

function UploadProgressCard({ upload, onCancel }: UploadProgressCardProps) {
  const formatSpeed = (bytesPerSecond: number): string => {
    if (bytesPerSecond < 1024) return `${bytesPerSecond.toFixed(0)} B/s`
    if (bytesPerSecond < 1024 * 1024) return `${(bytesPerSecond / 1024).toFixed(1)} KB/s`
    return `${(bytesPerSecond / (1024 * 1024)).toFixed(1)} MB/s`
  }

  const formatTime = (seconds: number): string => {
    if (seconds < 60) return `${Math.ceil(seconds)} 秒`
    if (seconds < 3600) return `${Math.ceil(seconds / 60)} 分钟`
    const hours = Math.floor(seconds / 3600)
    const mins = Math.ceil((seconds % 3600) / 60)
    return `${hours} 小时 ${mins} 分钟`
  }

  return (
    <div className="bg-surface-container-lowest p-6 rounded-xl transition-all duration-300">
      <div className="flex items-start gap-4">
        <div className="w-12 h-12 bg-secondary-container rounded-lg flex items-center justify-center text-on-secondary-container">
          <Icon name="movie" size={24} />
        </div>
        <div className="flex-1">
          <div className="flex justify-between items-start mb-2">
            <div>
              <h4 className="font-semibold text-on-surface">{upload.file.name}</h4>
              <p className="text-xs text-slate-500 mt-1">{formatFileSize(upload.file.size)}</p>
            </div>
            <button
              onClick={onCancel}
              className="p-1 hover:bg-error-container hover:text-error rounded-md transition-colors"
              title="取消上传"
            >
              <Icon name="close" size={20} />
            </button>
          </div>

          <div className="mt-4">
            <ProgressBar progress={upload.progress} />
          </div>

          <div className="flex justify-between items-center mt-3">
            <span className="text-xs text-slate-500 flex items-center gap-1">
              <Icon name="speed" size={14} />
              {upload.status === 'initializing'
                ? '正在初始化...'
                : upload.status === 'completing'
                ? '正在完成上传...'
                : `上传速度: ${formatSpeed(upload.speed)} | 约 ${formatTime(upload.estimatedTime)}`}
            </span>
            <span className="text-sm font-bold text-primary">{Math.round(upload.progress)}%</span>
          </div>
        </div>
      </div>
    </div>
  )
}

interface CompletedUploadItemProps {
  upload: CompletedUpload
  onReviewMetadata: () => void
}

function CompletedUploadItem({ upload, onReviewMetadata }: CompletedUploadItemProps) {
  return (
    <div className="bg-surface-container-lowest p-5 rounded-xl flex items-center gap-4 group hover:bg-surface-bright transition-all">
      <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center text-green-600">
        <Icon name="check_circle" size={20} />
      </div>
      <div className="flex-1 flex items-center justify-between">
        <div>
          <h4 className="font-medium text-sm text-on-surface">{upload.fileName}</h4>
          <p className="text-xs text-green-600 mt-0.5 font-medium flex items-center gap-1">
            上传完成
          </p>
        </div>
        <div className="flex items-center gap-6">
          <div className="w-32 h-1.5 bg-green-500 rounded-full" />
          <button
            onClick={onReviewMetadata}
            className="text-primary text-sm font-semibold hover:underline px-3 py-1 bg-primary/5 rounded transition-colors"
          >
            审核元数据
          </button>
        </div>
      </div>
    </div>
  )
}

function EditorialTipCards() {
  return (
    <section className="mt-16 grid grid-cols-3 gap-6">
      <div className="col-span-2 bg-gradient-to-br from-primary to-primary-container p-8 rounded-2xl text-white relative overflow-hidden">
        <div className="relative z-10">
          <h4 className="font-headline text-xl mb-2">如何提升视频曝光率？</h4>
          <p className="text-white/80 text-sm max-w-md mb-6 font-body">
            使用 Grace 的 AI 元数据增强工具，我们可以为您自动生成高流量标签和优化的视频描述。
          </p>
          <button className="bg-white text-primary px-6 py-2 rounded-lg font-bold text-sm hover:bg-blue-50 transition-colors">
            立即开启 AI 增强
          </button>
        </div>
        <span className="material-symbols-outlined absolute -bottom-4 -right-4 text-[120px] opacity-10 rotate-12">
          auto_awesome
        </span>
      </div>

      <div className="bg-tertiary-fixed p-8 rounded-2xl flex flex-col justify-between">
        <div>
          <h4 className="font-headline text-lg text-on-tertiary-fixed mb-2">分发渠道统计</h4>
          <p className="text-on-tertiary-fixed-variant text-xs">您已连接 8 个全球分发渠道</p>
        </div>
        <div className="flex justify-between items-end">
          <span className="text-4xl font-headline font-extrabold text-on-tertiary-fixed tracking-tight">
            85%
          </span>
          <span className="text-[10px] font-bold px-2 py-1 bg-on-tertiary-fixed text-white rounded-full">
            同步率极高
          </span>
        </div>
      </div>
    </section>
  )
}

export function VideoUploadPage() {
  const navigate = useNavigate()
  const [validationError, setValidationError] = useState<ValidationError | null>(null)
  const [uploadingFile, setUploadingFile] = useState<UploadingFile | null>(null)
  const [completedUploads, setCompletedUploads] = useState<CompletedUpload[]>([])
  const uploadAbortRef = useRef<boolean>(false)
  const startTimeRef = useRef<number>(0)

  const { addToast } = useAppStore()

  const initUploadMutation = useInitUpload()
  const uploadChunkMutation = useUploadChunk()
  const completeUploadMutation = useCompleteUpload()

  const uploadChunkWithRetry = useCallback(
    async (uploadId: string, chunkIndex: number, chunk: Blob): Promise<void> => {
      let attempts = 0

      while (attempts < MAX_RETRY_ATTEMPTS) {
        if (uploadAbortRef.current) {
          throw new Error('Upload cancelled')
        }

        try {
          await uploadChunkMutation.mutateAsync({ uploadId, chunkIndex, chunk })
          return
        } catch (error) {
          attempts++
          if (attempts >= MAX_RETRY_ATTEMPTS) {
            throw error
          }
          await new Promise((resolve) => setTimeout(resolve, 1000 * attempts))
        }
      }
    },
    [uploadChunkMutation]
  )

  const handleUpload = useCallback(
    async (file: File) => {
      uploadAbortRef.current = false
      startTimeRef.current = Date.now()

      try {
        const format = getVideoFormat(file.name)

        setUploadingFile({
          file,
          uploadId: null,
          progress: 0,
          uploadedChunks: 0,
          totalChunks: 0,
          speed: 0,
          estimatedTime: 0,
          status: 'initializing',
          error: null,
        })

        // Step 1: Initialize upload
        const initResponse = await initUploadMutation.mutateAsync({
          fileName: file.name,
          fileSize: file.size,
          format,
        })

        if (uploadAbortRef.current) return

        const { uploadId, totalChunks, chunkSize } = initResponse

        setUploadingFile((prev) =>
          prev
            ? {
                ...prev,
                uploadId,
                totalChunks,
                status: 'uploading',
              }
            : null
        )

        // Step 2: Upload chunks
        for (let i = 0; i < totalChunks; i++) {
          if (uploadAbortRef.current) {
            throw new Error('Upload cancelled')
          }

          const start = i * chunkSize
          const end = Math.min(start + chunkSize, file.size)
          const chunk = file.slice(start, end)

          await uploadChunkWithRetry(uploadId, i, chunk)

          const uploadedChunks = i + 1
          const progress = (uploadedChunks / totalChunks) * 100
          const elapsedMs = Date.now() - startTimeRef.current
          const uploadedBytes = uploadedChunks * chunkSize
          const speed = calculateUploadSpeed(uploadedBytes, elapsedMs)
          const remainingBytes = file.size - uploadedBytes
          const estimatedTime = estimateRemainingTime(remainingBytes, speed)

          setUploadingFile((prev) =>
            prev
              ? {
                  ...prev,
                  uploadedChunks,
                  progress,
                  speed,
                  estimatedTime,
                }
              : null
          )
        }

        if (uploadAbortRef.current) return

        // Step 3: Complete upload
        setUploadingFile((prev) =>
          prev
            ? {
                ...prev,
                status: 'completing',
              }
            : null
        )

        const completeResponse = await completeUploadMutation.mutateAsync(uploadId)

        setUploadingFile((prev) =>
          prev
            ? {
                ...prev,
                progress: 100,
                status: 'completed',
              }
            : null
        )

        // Add to completed uploads
        setCompletedUploads((prev) => [
          {
            videoId: completeResponse.videoId,
            fileName: completeResponse.fileName,
            fileSize: completeResponse.fileSize,
            completedAt: new Date(),
          },
          ...prev,
        ])

        addToast({
          type: 'success',
          message: `「${completeResponse.fileName}」上传成功`,
        })

        // Clear uploading file after a delay
        setTimeout(() => {
          setUploadingFile(null)
        }, 2000)
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : '上传失败'
        setUploadingFile((prev) =>
          prev
            ? {
                ...prev,
                status: 'error',
                error: errorMessage,
              }
            : null
        )
        addToast({
          type: 'error',
          message: errorMessage,
        })
      }
    },
    [initUploadMutation, uploadChunkWithRetry, completeUploadMutation, addToast]
  )

  const handleFileSelect = useCallback(
    (file: File) => {
      if (uploadingFile && uploadingFile.status !== 'completed' && uploadingFile.status !== 'error') {
        addToast({
          type: 'info',
          message: '请等待当前上传完成',
        })
        return
      }
      handleUpload(file)
    },
    [handleUpload, uploadingFile, addToast]
  )

  const handleValidationError = useCallback(
    (error: ValidationError) => {
      setValidationError(error)
      setTimeout(() => setValidationError(null), 5000)
    },
    []
  )

  const handleCloseError = useCallback(() => {
    setValidationError(null)
  }, [])

  const handleCancelUpload = useCallback(() => {
    uploadAbortRef.current = true
    setUploadingFile(null)
    addToast({
      type: 'info',
      message: '上传已取消',
    })
  }, [addToast])

  const handleReviewMetadata = useCallback(
    (videoId: string) => {
      navigate(APP_ROUTES.VIDEO_METADATA_DETAIL.replace(':videoId', videoId))
    },
    [navigate]
  )

  const isUploading = uploadingFile && uploadingFile.status !== 'completed' && uploadingFile.status !== 'error'

  return (
    <div className="p-8 max-w-5xl mx-auto">
      {/* Page Header */}
      <div className="mb-12">
        <h1 className="font-headline text-[2.75rem] font-bold text-on-surface tracking-tight">
          上传视频
        </h1>
        <p className="text-slate-500 mt-2 font-body">
          将您的烹饪灵感分享给世界，支持多平台一键分发。
        </p>
      </div>

      {/* Upload Area Section */}
      <section className="bg-surface-container-lowest rounded-xl p-4 mb-8">
        <DropZone
          onFileSelect={handleFileSelect}
          onValidationError={handleValidationError}
          disabled={!!isUploading}
        />
      </section>

      {/* Upload Progress Section */}
      {uploadingFile && (
        <section className="mb-12">
          <h3 className="font-headline text-lg font-bold mb-4 flex items-center gap-2">
            <span className="w-2 h-2 bg-primary rounded-full" />
            正在上传
          </h3>
          <UploadProgressCard
            upload={uploadingFile}
            onCancel={handleCancelUpload}
          />
        </section>
      )}

      {/* Completed Uploads Section */}
      {completedUploads.length > 0 && (
        <section className="mb-12">
          <h3 className="font-headline text-lg font-bold mb-4 flex items-center gap-2">
            <span className="w-2 h-2 bg-green-500 rounded-full" />
            已处理
          </h3>
          <div className="grid grid-cols-1 gap-4">
            {completedUploads.map((upload) => (
              <CompletedUploadItem
                key={upload.videoId}
                upload={upload}
                onReviewMetadata={() => handleReviewMetadata(upload.videoId)}
              />
            ))}
          </div>
        </section>
      )}

      {/* Editorial Tip Cards */}
      <EditorialTipCards />

      {/* Validation Error Toast */}
      <ValidationErrorToast error={validationError} onClose={handleCloseError} />
    </div>
  )
}
