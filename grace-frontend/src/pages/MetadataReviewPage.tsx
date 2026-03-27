import { useState, useCallback } from 'react'
import { useParams } from 'react-router-dom'
import { useVideoDetail } from '@/hooks/useVideos'
import {
  useVideoMetadata,
  useUpdateMetadata,
  useRegenerateMetadata,
  useConfirmMetadata,
} from '@/hooks/useMetadata'
import { Icon } from '@/components/ui/Icon'
import { formatFileSize } from '@/utils/format'
import type { VideoMetadata } from '@/types/metadata'

// Constants
const MAX_TITLE_LENGTH = 100
const MAX_DESCRIPTION_LENGTH = 5000
const MIN_TAGS_COUNT = 5

// AiBadge Component
function AiBadge() {
  return (
    <span className="inline-flex items-center gap-1 bg-secondary-fixed text-on-secondary-fixed px-2.5 py-1 rounded-full text-xs font-medium">
      <Icon name="auto_awesome" size={14} />
      AI 生成
    </span>
  )
}

// AiBadge with text variant
function AiBadgeWithText({ text }: { text: string }) {
  return (
    <span className="inline-flex items-center gap-1.5 bg-secondary-fixed text-on-secondary-fixed-variant px-3 py-1 rounded-full text-xs font-bold">
      <Icon name="auto_awesome" size={16} />
      {text}
    </span>
  )
}

// TagChip Component
interface TagChipProps {
  tag: string
  onRemove: () => void
  disabled?: boolean
}

function TagChip({ tag, onRemove, disabled }: TagChipProps) {
  return (
    <span className="inline-flex items-center gap-1.5 bg-tertiary-fixed text-on-tertiary-fixed px-3 py-1.5 rounded-full text-xs font-medium group transition-all hover:bg-tertiary-fixed-dim">
      {tag}
      {!disabled && (
        <button
          onClick={onRemove}
          className="inline-flex items-center justify-center hover:text-error transition-colors rounded-full p-0.5 hover:bg-tertiary/20"
          aria-label={`删除标签 ${tag}`}
        >
          <Icon name="close" size={14} />
        </button>
      )}
    </span>
  )
}

// Character Counter Component
interface CharCounterProps {
  current: number
  max: number
}

function CharCounter({ current, max }: CharCounterProps) {
  const isOverLimit = current > max
  return (
    <span className={`text-xs ${isOverLimit ? 'text-error' : 'text-on-surface-variant'}`}>
      {current} / {max}
    </span>
  )
}

// Video Preview Card Component
interface VideoPreviewCardProps {
  thumbnailUrl: string | null
  fileName: string
  format: string
  fileSize: number
  duration: string
}

function VideoPreviewCard({
  thumbnailUrl,
  fileName,
  format,
  fileSize,
  duration,
}: VideoPreviewCardProps) {
  return (
    <div className="bg-surface-container-lowest rounded-lg overflow-hidden">
      {/* Video Player Area */}
      <div className="aspect-video relative bg-on-surface rounded-t-lg overflow-hidden group">
        {thumbnailUrl ? (
          <img
            src={thumbnailUrl}
            alt={fileName}
            className="w-full h-full object-cover opacity-80 group-hover:scale-105 transition-transform duration-700"
          />
        ) : (
          <div className="w-full h-full bg-surface-container-high flex items-center justify-center">
            <Icon name="video_file" size={64} className="text-on-surface-variant" />
          </div>
        )}
        {/* Play Button Overlay */}
        <div className="absolute inset-0 flex items-center justify-center">
          <button
            className="w-16 h-16 bg-black/50 backdrop-blur-sm rounded-full flex items-center justify-center text-white border border-white/30 hover:scale-110 transition-transform active:scale-95"
            aria-label="播放视频"
          >
            <Icon name="play_arrow" size={32} className="translate-x-0.5" />
          </button>
        </div>
        {/* Progress Bar at Bottom */}
        <div className="absolute bottom-0 left-0 right-0 h-1 bg-white/30">
          <div className="h-full w-1/3 bg-primary shadow-[0_0_8px_rgba(0,87,194,0.6)]" />
        </div>
      </div>

      {/* Video Info Grid */}
      <div className="p-6 grid grid-cols-2 gap-4">
        {/* File Name */}
        <div className="flex flex-col gap-1">
          <span className="font-label text-xs text-on-surface-variant uppercase tracking-wider">
            文件名
          </span>
          <span className="font-body text-sm font-medium text-on-surface mt-1 truncate" title={fileName}>
            {fileName}
          </span>
        </div>

        {/* Format */}
        <div className="flex flex-col gap-1">
          <span className="font-label text-xs text-on-surface-variant uppercase tracking-wider">
            格式
          </span>
          <span className="font-body text-sm font-medium text-on-surface mt-1">
            {format}
          </span>
        </div>

        {/* File Size */}
        <div className="flex flex-col gap-1">
          <span className="font-label text-xs text-on-surface-variant uppercase tracking-wider">
            文件大小
          </span>
          <span className="font-body text-sm font-medium text-on-surface mt-1">
            {formatFileSize(fileSize)}
          </span>
        </div>

        {/* Duration */}
        <div className="flex flex-col gap-1">
          <span className="font-label text-xs text-on-surface-variant uppercase tracking-wider">
            时长
          </span>
          <span className="font-body text-sm font-medium text-on-surface mt-1">
            {duration}
          </span>
        </div>
      </div>
    </div>
  )
}

// Metadata Editor Card Component
interface MetadataEditorCardProps {
  metadata: VideoMetadata | null
  videoStatus: string
  onConfirmed: () => void
}

function MetadataEditorCard({ metadata, videoStatus, onConfirmed }: MetadataEditorCardProps) {
  // Use metadata values directly for initial state
  const [title, setTitle] = useState(metadata?.title ?? '')
  const [description, setDescription] = useState(metadata?.description ?? '')
  const [tags, setTags] = useState<string[]>(metadata?.tags ?? [])
  const [newTagInput, setNewTagInput] = useState('')
  const [isDirty, setIsDirty] = useState(false)
  const [showConfirmDialog, setShowConfirmDialog] = useState(false)

  const updateMutation = useUpdateMetadata()
  const regenerateMutation = useRegenerateMetadata()
  const confirmMutation = useConfirmMetadata()

  // Check if metadata is confirmed (read-only)
  const isConfirmed = videoStatus === 'READY_TO_PUBLISH' || 
                      videoStatus === 'PUBLISHING' || 
                      videoStatus === 'PUBLISHED' || 
                      videoStatus === 'PROMOTION_DONE'

  // Handle title change
  const handleTitleChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    setTitle(e.target.value)
    setIsDirty(true)
  }, [])

  // Handle description change
  const handleDescriptionChange = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setDescription(e.target.value)
    setIsDirty(true)
  }, [])

  // Handle tag remove
  const handleRemoveTag = useCallback((tagToRemove: string) => {
    setTags(prev => prev.filter(tag => tag !== tagToRemove))
    setIsDirty(true)
  }, [])

  // Handle tag add
  const handleAddTag = useCallback(() => {
    const trimmedTag = newTagInput.trim()
    if (trimmedTag && !tags.includes(trimmedTag)) {
      setTags(prev => [...prev, trimmedTag])
      setNewTagInput('')
      setIsDirty(true)
    }
  }, [newTagInput, tags])

  // Handle tag input keydown
  const handleTagKeyDown = useCallback((e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      handleAddTag()
    }
  }, [handleAddTag])

  // Handle save draft
  const handleSaveDraft = useCallback(async () => {
    if (!metadata) return

    try {
      await updateMutation.mutateAsync({
        metadataId: metadata.metadataId,
        request: {
          title,
          description,
          tags,
        },
      })
      setIsDirty(false)
      // Show success toast (using alert for now, can be replaced with toast component)
      alert('保存成功')
    } catch (error) {
      console.warn('Failed to save draft:', error)
      alert('保存失败，请重试')
    }
  }, [metadata, title, description, tags, updateMutation])

  // Handle regenerate
  const handleRegenerate = useCallback(async () => {
    if (!metadata) return

    try {
      await regenerateMutation.mutateAsync(metadata.metadataId)
      alert('元数据已重新生成')
    } catch (error) {
      console.warn('Failed to regenerate:', error)
      alert('重新生成失败，请重试')
    }
  }, [metadata, regenerateMutation])

  // Handle confirm
  const handleConfirm = useCallback(async () => {
    if (!metadata) return

    // Validate minimum tags
    if (tags.length < MIN_TAGS_COUNT) {
      alert(`至少需要 ${MIN_TAGS_COUNT} 个标签`)
      return
    }

    // Validate title and description length
    if (title.length > MAX_TITLE_LENGTH) {
      alert(`标题不能超过 ${MAX_TITLE_LENGTH} 字符`)
      return
    }

    if (description.length > MAX_DESCRIPTION_LENGTH) {
      alert(`描述不能超过 ${MAX_DESCRIPTION_LENGTH} 字符`)
      return
    }

    try {
      await confirmMutation.mutateAsync(metadata.metadataId)
      setShowConfirmDialog(false)
      alert('元数据已确认')
      onConfirmed()
    } catch (error) {
      console.warn('Failed to confirm:', error)
      alert('确认失败，请重试')
    }
  }, [metadata, title, description, tags, confirmMutation, onConfirmed])

  // Loading state
  if (!metadata) {
    return (
      <div className="bg-surface-container-lowest rounded-lg p-8 min-h-[500px] flex flex-col items-center justify-center">
        <Icon name="auto_awesome" size={48} className="text-on-surface-variant mb-4" />
        <p className="font-body text-on-surface-variant">正在生成元数据...</p>
        <p className="font-body text-xs text-on-surface-variant mt-2">
          AI 正在分析视频内容，请稍候
        </p>
      </div>
    )
  }

  return (
    <div className="bg-surface-container-lowest rounded-lg overflow-hidden">
      {/* Editor Content */}
      <div className="p-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <h2 className="font-headline text-xl font-bold text-on-surface">
            内容元数据编辑
          </h2>
          <AiBadgeWithText text="AI 建议已生成" />
        </div>

        {/* Confirmed Notice */}
        {isConfirmed && (
          <div className="mb-6 p-4 bg-primary-fixed-dim/30 rounded-lg flex items-center gap-3">
            <Icon name="check_circle" size={20} className="text-primary" />
            <span className="font-body text-sm text-on-surface">
              元数据已确认，无法编辑
            </span>
          </div>
        )}

        {/* Form Fields */}
        <div className="space-y-8">
          {/* Title Field */}
          <div className="flex flex-col gap-2">
            <div className="flex justify-between items-end">
              <label className="font-label text-xs font-medium text-on-surface-variant uppercase tracking-wider">
                视频标题
              </label>
              <CharCounter current={title.length} max={MAX_TITLE_LENGTH} />
            </div>
            <input
              type="text"
              value={title}
              onChange={handleTitleChange}
              disabled={isConfirmed}
              placeholder="输入视频标题"
              className="mt-2 w-full bg-surface-container-low rounded-md px-4 py-3 text-sm font-body text-on-surface placeholder:text-outline focus:ring-2 focus:ring-primary/40 focus:outline-none transition-all disabled:opacity-60 disabled:cursor-not-allowed"
            />
            <p className="text-xs text-primary flex items-center gap-1 mt-1">
              <Icon name="info" size={14} />
              YouTube 标题建议 60 字符以内，以获得更好的 SEO 排名
            </p>
          </div>

          {/* Description Field */}
          <div className="flex flex-col gap-2">
            <div className="flex justify-between items-end">
              <label className="font-label text-xs font-medium text-on-surface-variant uppercase tracking-wider">
                视频描述
              </label>
              <CharCounter current={description.length} max={MAX_DESCRIPTION_LENGTH} />
            </div>
            <textarea
              value={description}
              onChange={handleDescriptionChange}
              disabled={isConfirmed}
              placeholder="输入视频描述"
              rows={6}
              className="mt-2 w-full bg-surface-container-low rounded-md px-4 py-3 text-sm font-body text-on-surface placeholder:text-outline focus:ring-2 focus:ring-primary/40 focus:outline-none transition-all resize-none disabled:opacity-60 disabled:cursor-not-allowed"
            />
          </div>

          {/* Tags Field */}
          <div className="flex flex-col gap-3">
            <label className="font-label text-xs font-medium text-on-surface-variant uppercase tracking-wider">
              搜索标签
              <span className="ml-2 text-on-surface-variant/60">
                ({tags.length} 个，至少需要 {MIN_TAGS_COUNT} 个)
              </span>
            </label>
            <div className="flex flex-wrap gap-2 p-4 bg-surface-container-low rounded-lg min-h-[100px] items-start content-start">
              {tags.map((tag) => (
                <TagChip
                  key={tag}
                  tag={tag}
                  onRemove={() => handleRemoveTag(tag)}
                  disabled={isConfirmed}
                />
              ))}
              {!isConfirmed && (
                <input
                  type="text"
                  value={newTagInput}
                  onChange={(e) => setNewTagInput(e.target.value)}
                  onKeyDown={handleTagKeyDown}
                  placeholder="添加标签..."
                  className="bg-transparent border-none focus:ring-0 px-3 py-1.5 text-xs w-32 text-on-surface placeholder:text-outline focus:outline-none"
                />
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Action Footer */}
      {!isConfirmed && (
        <div className="px-8 pb-8">
          <div className="h-px bg-surface-container-high w-full mb-6" />
          <div className="flex items-center justify-between">
            {/* Left: Regenerate */}
            <button
              onClick={handleRegenerate}
              disabled={regenerateMutation.isPending}
              className="flex items-center gap-2 text-primary text-sm font-semibold hover:opacity-80 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Icon name="refresh" size={18} />
              {regenerateMutation.isPending ? '生成中...' : '重新生成'}
            </button>

            {/* Right: Save & Confirm */}
            <div className="flex items-center gap-3">
              <button
                onClick={handleSaveDraft}
                disabled={updateMutation.isPending || !isDirty}
                className="px-6 py-2.5 rounded-lg border border-outline-variant text-on-surface-variant font-bold text-sm hover:bg-surface-container-low transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {updateMutation.isPending ? '保存中...' : '保存草稿'}
              </button>
              <button
                onClick={() => setShowConfirmDialog(true)}
                disabled={confirmMutation.isPending}
                className="px-8 py-2.5 rounded-lg bg-gradient-to-r from-primary to-primary-container text-white font-bold text-sm shadow-[0_4px_12px_rgba(0,87,194,0.25)] hover:shadow-[0_6px_16px_rgba(0,87,194,0.35)] transition-all active:scale-95 flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Icon name="check_circle" size={18} />
                {confirmMutation.isPending ? '确认中...' : '确认元数据'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirm Dialog */}
      {showConfirmDialog && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-surface rounded-lg p-6 max-w-md w-full mx-4 shadow-2xl">
            <h3 className="font-headline text-lg font-bold text-on-surface mb-2">
              确认元数据
            </h3>
            <p className="font-body text-sm text-on-surface-variant mb-6">
              确认后元数据将无法再次编辑，视频状态将变为&quot;待发布&quot;。是否确认？
            </p>
            <div className="flex items-center justify-end gap-3">
              <button
                onClick={() => setShowConfirmDialog(false)}
                className="px-4 py-2 rounded-lg text-on-surface-variant font-medium text-sm hover:bg-surface-container-low transition-all"
              >
                取消
              </button>
              <button
                onClick={handleConfirm}
                className="px-6 py-2 rounded-lg bg-primary text-white font-medium text-sm hover:opacity-90 transition-all"
              >
                确认
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// Loading Skeleton Component
function VideoPreviewSkeleton() {
  return (
    <div className="bg-surface-container-lowest rounded-lg overflow-hidden">
      <div className="aspect-video bg-surface-container-high animate-pulse" />
      <div className="p-6 grid grid-cols-2 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="flex flex-col gap-1">
            <div className="h-3 w-12 bg-surface-container-high rounded animate-pulse" />
            <div className="h-4 w-24 bg-surface-container-high rounded mt-1 animate-pulse" />
          </div>
        ))}
      </div>
    </div>
  )
}

// Main Page Component
export default function MetadataReviewPage() {
  const { videoId } = useParams<{ videoId: string }>()
  const { data: video, isLoading, error } = useVideoDetail(videoId ?? '')
  const { data: metadata } = useVideoMetadata(videoId ?? '')

  // Handle confirmed callback
  const handleConfirmed = useCallback(() => {
    // Video status will be updated via cache invalidation
    // Optionally navigate to publish page
    // navigate(`/videos/${videoId}/publish`)
  }, [])

  // Error State
  if (error) {
    return (
      <div className="min-h-[calc(100vh-8rem)] flex items-center justify-center">
        <div className="text-center">
          <Icon name="error" size={48} className="text-error mx-auto mb-4" />
          <h2 className="font-headline text-xl font-bold text-on-surface mb-2">
            加载失败
          </h2>
          <p className="font-body text-sm text-on-surface-variant">
            {error instanceof Error ? error.message : '无法加载视频信息'}
          </p>
        </div>
      </div>
    )
  }

  // Video Not Found State
  if (!isLoading && !video) {
    return (
      <div className="min-h-[calc(100vh-8rem)] flex items-center justify-center">
        <div className="text-center">
          <Icon name="video_file" size={48} className="text-on-surface-variant mx-auto mb-4" />
          <h2 className="font-headline text-xl font-bold text-on-surface mb-2">
            视频不存在
          </h2>
          <p className="font-body text-sm text-on-surface-variant">
            该视频可能已被删除或您没有访问权限
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div>
            <h1 className="font-headline text-2xl font-bold text-on-surface">
              元数据审核
            </h1>
            <p className="font-body text-sm text-on-surface-variant mt-1">
              审核并编辑 AI 生成的视频元数据
            </p>
          </div>
          <AiBadge />
        </div>
      </div>

      {/* Two Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Left Column: Video Preview */}
        <section className="flex flex-col gap-6">
          {isLoading ? (
            <VideoPreviewSkeleton />
          ) : video ? (
            <VideoPreviewCard
              thumbnailUrl={video.thumbnailUrl}
              fileName={video.fileName}
              format={video.format}
              fileSize={video.fileSize}
              duration={video.duration}
            />
          ) : null}
        </section>

        {/* Right Column: Metadata Editor */}
        <section>
          <MetadataEditorCard
            key={metadata?.metadataId ?? video?.metadata?.metadataId ?? 'empty'}
            metadata={metadata ?? video?.metadata ?? null}
            videoStatus={video?.status ?? ''}
            onConfirmed={handleConfirmed}
          />
        </section>
      </div>
    </div>
  )
}
