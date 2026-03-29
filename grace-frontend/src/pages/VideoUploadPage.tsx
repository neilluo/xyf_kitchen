import { useState, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Icon } from '@/components/ui/Icon'
import { ProgressBar } from '@/components/ui/ProgressBar'
import { useOssUpload } from '@/hooks/useOssUpload'
import { useVideoList } from '@/hooks/useVideos'
import { useAppStore } from '@/store/useAppStore'
import { formatFileSize } from '@/utils/format'
import { ROUTES } from '@/utils/constants'
import type { VideoStatus, Video } from '@/types/video'

const SUPPORTED_FORMATS = ['mp4', 'mov', 'avi', 'mkv']
const MAX_FILE_SIZE = 5 * 1024 * 1024 * 1024

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

interface CompletedUpload {
  videoId: string
  fileName: string
  fileSize: number
  completedAt: Date
  status: VideoStatus
  thumbnailUrl?: string
}

const statusTextMap: Record<VideoStatus, string> = {
  UPLOADED: '上传完成，正在生成元数据...',
  METADATA_GENERATED: '元数据已生成，等待审核',
  READY_TO_PUBLISH: '准备就绪，等待发布',
  PUBLISHING: '正在发布...',
  PUBLISHED: '已发布',
  PUBLISH_FAILED: '发布失败',
  PROMOTION_DONE: '推广完成',
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

  const handleButtonClick = useCallback((e: React.MouseEvent) => {
    e.stopPropagation()
    if (!disabled) fileInputRef.current?.click()
  }, [disabled])

  return (
    <div
      className={`upload-dashed-border p-16 flex flex-col items-center justify-center text-center ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
      style={{
        backgroundColor: isDragging ? 'rgba(0, 87, 194, 0.1)' : 'rgba(240, 245, 255, 0.5)',
      }}
      onDragEnter={handleDragEnter}
      onDragLeave={handleDragLeave}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
      onClick={handleClick}
    >
      <div className="w-20 h-20 bg-primary-fixed rounded-full flex items-center justify-center mb-6">
        <Icon
          name="cloud_upload"
          size={48}
          className="text-primary"
        />
      </div>

      <h2 className="text-xl font-headline font-bold mb-2 text-on-surface">
        拖拽视频文件到此处，或点击选择文件
      </h2>

      <p className="text-slate-500 mb-8 max-w-sm">
        支持 MP4、MOV、AVI、MKV，不超过 5GB。建议使用 1080p 或更高分辨率以获得最佳展示效果。
      </p>

      <button
        onClick={handleButtonClick}
        className="px-8 py-3 rounded-lg border-2 border-primary text-primary font-semibold hover:bg-primary/5 transition-all flex items-center gap-2"
      >
        <Icon name="add_circle" size={20} />
        选择文件
      </button>

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
  fileName: string
  fileSize: number
  progress: number
  speed: number
  estimatedTime: number
  status: 'initializing' | 'uploading' | 'completing' | 'completed' | 'error'
  error: Error | null
  onCancel: () => void
}

function UploadProgressCard({ fileName, fileSize, progress, speed, estimatedTime, status, error, onCancel }: UploadProgressCardProps) {
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

  const statusText = status === 'initializing'
    ? '正在初始化...'
    : status === 'completing'
    ? '正在完成上传...'
    : status === 'error'
    ? error?.message ?? '上传失败'
    : `上传速度: ${formatSpeed(speed)} | 约 ${formatTime(estimatedTime)}`

  return (
    <div className="bg-surface-container-lowest p-6 rounded-xl transition-all duration-300">
      <div className="flex items-start gap-4">
        <div className="w-12 h-12 bg-secondary-container rounded-lg flex items-center justify-center text-on-secondary-container">
          <Icon name="movie" size={24} />
        </div>
        <div className="flex-1">
          <div className="flex justify-between items-start mb-2">
            <div>
              <h4 className="font-semibold text-on-surface">{fileName}</h4>
              <p className="text-xs text-slate-500 mt-1">{formatFileSize(fileSize)}</p>
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
            <ProgressBar progress={progress} />
          </div>

          <div className="flex justify-between items-center mt-3">
            <span className="text-xs text-slate-500 flex items-center gap-1">
              <Icon name="speed" size={14} />
              {statusText}
            </span>
            <span className="text-sm font-bold text-primary">{Math.round(progress)}%</span>
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
  const statusText = statusTextMap[upload.status]
  const isMetadataPending = upload.status === 'UPLOADED'

  return (
    <div className="bg-surface-container-lowest p-5 rounded-xl flex items-center gap-4 group hover:bg-surface-bright transition-all">
      <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center text-green-600">
        <Icon name="check_circle" size={20} />
      </div>
      <div className="flex-1 flex items-center justify-between">
        <div>
          <h4 className="font-medium text-sm text-on-surface">{upload.fileName}</h4>
          <p className={`text-xs mt-0.5 font-medium flex items-center gap-1 ${isMetadataPending ? 'text-green-600' : 'text-slate-500'}`}>
            {isMetadataPending ? statusText : `已于 ${formatCompletedTime(upload.completedAt)} 上传`}
          </p>
        </div>
        <div className="flex items-center gap-6">
          <div className="w-32 h-1.5 bg-green-500 rounded-full" />
          <button
            onClick={onReviewMetadata}
            className="text-primary text-sm font-semibold hover:underline px-3 py-1 bg-primary/5 rounded transition-colors"
          >
            编辑元数据
          </button>
        </div>
      </div>
    </div>
  )
}

function formatCompletedTime(date: Date): string {
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))

  if (diffDays === 0) {
    const hours = Math.floor(diffMs / (1000 * 60 * 60))
    if (hours === 0) {
      return '刚刚'
    }
    return `今天 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  } else if (diffDays === 1) {
    return `昨天 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
  } else if (diffDays < 7) {
    return `${diffDays} 天前`
  }
  return `${date.getMonth() + 1}/${date.getDate()}`
}

interface HistoryRecordItemProps {
  video: Video
}

function HistoryRecordItem({ video }: HistoryRecordItemProps) {
  const isPublished = video.status === 'PUBLISHED' || video.status === 'PROMOTION_DONE'

  return (
    <div className="bg-surface-container-lowest p-5 rounded-xl flex items-center gap-4 group hover:bg-surface-bright transition-all opacity-80">
      <div className="w-10 h-10 bg-slate-100 rounded-lg flex items-center justify-center text-slate-500">
        <Icon name="description" size={20} />
      </div>
      <div className="flex-1 flex items-center justify-between">
        <div>
          <h4 className="font-medium text-sm text-on-surface">{video.fileName}</h4>
          <p className="text-xs text-slate-500 mt-0.5">
            已于 {formatCompletedTime(new Date(video.createdAt))} 上传
          </p>
        </div>
        <div className="flex items-center gap-3">
          {isPublished && (
            <>
              <div className="flex -space-x-2">
                <div className="w-6 h-6 rounded-full bg-red-500 flex items-center justify-center text-[10px] text-white ring-2 ring-white">YT</div>
                <div className="w-6 h-6 rounded-full bg-blue-500 flex items-center justify-center text-[10px] text-white ring-2 ring-white">FB</div>
                <div className="w-6 h-6 rounded-full bg-pink-500 flex items-center justify-center text-[10px] text-white ring-2 ring-white">IG</div>
              </div>
              <span className="text-xs text-slate-400">已发布</span>
            </>
          )}
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
  const [completedUploads, setCompletedUploads] = useState<CompletedUpload[]>([])
  const [isUploading, setIsUploading] = useState(false)

  const { addToast } = useAppStore()
  const { uploadState, uploadFile, cancelUpload, resetState } = useOssUpload()
  const { data: videoListData } = useVideoList({ pageSize: 5, sort: 'createdAt', order: 'desc' })

  const handleUpload = useCallback(
    async (file: File) => {
      setIsUploading(true)
      try {
        const result = await uploadFile(file)
        setCompletedUploads((prev) => [
          {
            videoId: result.videoId,
            fileName: result.fileName,
            fileSize: file.size,
            completedAt: new Date(),
            status: uploadState.videoStatus ?? 'UPLOADED',
          },
          ...prev,
        ])
        addToast({
          type: 'success',
          message: `「${result.fileName}」上传成功`,
        })
        setTimeout(() => {
          resetState()
          setIsUploading(false)
        }, 2000)
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : '上传失败'
        addToast({
          type: 'error',
          message: errorMessage,
        })
        setIsUploading(false)
      }
    },
    [uploadFile, uploadState.videoStatus, addToast, resetState]
  )

  const handleFileSelect = useCallback(
    (file: File) => {
      if (isUploading) {
        addToast({
          type: 'info',
          message: '请等待当前上传完成',
        })
        return
      }
      handleUpload(file)
    },
    [handleUpload, isUploading, addToast]
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
    cancelUpload()
    setIsUploading(false)
    addToast({
      type: 'info',
      message: '上传已取消',
    })
  }, [cancelUpload, addToast])

  const handleReviewMetadata = useCallback(
    (videoId: string) => {
      navigate(ROUTES.VIDEO_METADATA(videoId))
    },
    [navigate]
  )

  const showProgress =
    uploadState.status !== 'idle' &&
    uploadState.status !== 'completed' &&
    uploadState.status !== 'error'

  return (
    <div className="p-8 max-w-5xl mx-auto">
      <div className="mb-12">
        <h1 className="font-headline text-[2.75rem] font-bold text-on-surface tracking-tight">
          发布您的佳作
        </h1>
        <p className="text-slate-500 mt-2 font-body">
          将您的烹饪灵感分享给世界，支持多平台一键分发。
        </p>
      </div>

      <section className="bg-surface-container-lowest rounded-xl p-4 mb-8">
        <DropZone
          onFileSelect={handleFileSelect}
          onValidationError={handleValidationError}
          disabled={isUploading}
        />
      </section>

      {showProgress && (
        <section className="mb-12">
          <h3 className="font-headline text-lg font-bold mb-4 flex items-center gap-2">
            <span className="w-2 h-2 bg-primary rounded-full" />
            正在上传 (1)
          </h3>
          <UploadProgressCard
            fileName={uploadState.fileName}
            fileSize={uploadState.fileSize}
            progress={uploadState.progress}
            speed={uploadState.speed}
            estimatedTime={uploadState.estimatedTime}
            status={uploadState.status}
            error={uploadState.error}
            onCancel={handleCancelUpload}
          />
        </section>
      )}

      {(completedUploads.length > 0 || (videoListData?.items && videoListData.items.length > 0)) && (
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
            {videoListData?.items
              ?.filter((video) => !completedUploads.some((u) => u.videoId === video.videoId))
              .slice(0, 3)
              .map((video) => (
                <HistoryRecordItem key={video.videoId} video={video} />
              ))}
          </div>
        </section>
      )}

      <EditorialTipCards />

      <ValidationErrorToast error={validationError} onClose={handleCloseError} />
    </div>
  )
}