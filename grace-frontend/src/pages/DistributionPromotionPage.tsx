import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Icon } from '@/components/ui/Icon'
import { Card } from '@/components/ui/Card'
import { Input, Textarea } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { TagChip } from '@/components/ui/TagChip'
import { Button } from '@/components/ui/Button'
import { useVideoDetail } from '@/hooks/useVideos'
import { usePlatforms, usePublish, usePublishStatus } from '@/hooks/useDistribution'
import { useGenerateCopy, useExecutePromotion } from '@/hooks/usePromotions'
import type { VideoDetail } from '@/types/video'
import type { DistributionPlatform } from '@/types/distribution'
import type { PromotionItem } from '@/types/promotion'

type StepId = 1 | 2 | 3

interface StepConfig {
  id: StepId
  label: string
}

const STEPS: StepConfig[] = [
  { id: 1, label: '确认信息' },
  { id: 2, label: '选择平台' },
  { id: 3, label: '推广配置' },
]

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`
}

interface StepIndicatorProps {
  steps: StepConfig[]
  currentStep: StepId
}

function StepIndicator({ steps, currentStep }: StepIndicatorProps) {
  const getStepState = (stepId: StepId): 'completed' | 'active' | 'future' => {
    if (stepId < currentStep) return 'completed'
    if (stepId === currentStep) return 'active'
    return 'future'
  }

  const getDotClasses = (state: 'completed' | 'active' | 'future'): string => {
    switch (state) {
      case 'completed':
        return 'w-10 h-10 rounded-full bg-primary flex items-center justify-center text-white'
      case 'active':
        return 'w-10 h-10 rounded-full bg-primary ring-4 ring-primary-fixed flex items-center justify-center text-white font-bold shadow-lg shadow-primary/20'
      case 'future':
        return 'w-10 h-10 rounded-full bg-surface-container-high flex items-center justify-center text-on-surface-variant font-medium'
    }
  }

  const getLineClasses = (stepId: StepId): string => {
    const state = getStepState((stepId + 1) as StepId)
    if (state === 'completed' || state === 'active') {
      return 'h-0.5 w-24 bg-primary'
    }
    return 'h-0.5 w-24 bg-surface-container-high'
  }

  const getLabelClasses = (state: 'completed' | 'active' | 'future'): string => {
    switch (state) {
      case 'completed':
        return 'text-sm font-medium text-on-surface'
      case 'active':
        return 'text-sm font-bold text-primary'
      case 'future':
        return 'text-sm font-medium text-on-surface-variant'
    }
  }

  return (
    <div className="flex items-center justify-center mb-12">
      <div className="flex items-center">
        {steps.map((step, index) => {
          const state = getStepState(step.id)
          const isLast = index === steps.length - 1

          return (
            <div key={step.id} className="flex items-center">
              <div className="flex flex-col items-center gap-2 bg-background px-4">
                <div className={getDotClasses(state)}>
                  {state === 'completed' ? (
                    <Icon name="check" size={20} />
                  ) : (
                    <span>{step.id}</span>
                  )}
                </div>
                <span className={getLabelClasses(state)}>{step.label}</span>
              </div>
              {!isLast && <div className={getLineClasses(step.id)} />}
            </div>
          )
        })}
      </div>
    </div>
  )
}

interface VideoPreviewCardProps {
  video: VideoDetail
}

function VideoPreviewCard({ video }: VideoPreviewCardProps) {
  return (
    <Card className="p-6">
      <div className="flex gap-6">
        <div className="w-48 h-32 rounded-lg bg-surface-container-high flex items-center justify-center overflow-hidden">
          {video.thumbnailUrl ? (
            <img
              src={video.thumbnailUrl}
              alt={video.fileName}
              className="w-full h-full object-cover"
            />
          ) : (
            <Icon name="video_library" size={48} className="text-on-surface-variant" />
          )}
        </div>
        <div className="flex-1">
          <h3 className="font-body text-sm font-bold text-on-surface mb-3">
            {video.fileName}
          </h3>
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <span className="text-on-surface-variant">格式：</span>
              <span className="text-on-surface ml-2">{video.format}</span>
            </div>
            <div>
              <span className="text-on-surface-variant">大小：</span>
              <span className="text-on-surface ml-2">{formatFileSize(video.fileSize)}</span>
            </div>
            <div>
              <span className="text-on-surface-variant">时长：</span>
              <span className="text-on-surface ml-2">{video.duration}</span>
            </div>
            <div>
              <span className="text-on-surface-variant">状态：</span>
              <span className="text-on-surface ml-2">
                {video.status === 'READY_TO_PUBLISH' ? '待发布' : video.status}
              </span>
            </div>
          </div>
        </div>
      </div>
    </Card>
  )
}

interface MetadataSummaryCardProps {
  metadata: NonNullable<VideoDetail['metadata']>
}

function MetadataSummaryCard({ metadata }: MetadataSummaryCardProps) {
  return (
    <Card className="p-6 mt-6">
      <h3 className="font-body text-sm font-bold text-on-surface mb-4">元数据摘要</h3>
      <div className="space-y-4">
        <div>
          <label className="text-xs text-on-surface-variant font-bold uppercase tracking-wider mb-2 block">
            标题
          </label>
          <p className="text-sm text-on-surface">{metadata.title}</p>
        </div>
        <div>
          <label className="text-xs text-on-surface-variant font-bold uppercase tracking-wider mb-2 block">
            描述
          </label>
          <p className="text-sm text-on-surface leading-relaxed">{metadata.description}</p>
        </div>
        <div>
          <label className="text-xs text-on-surface-variant font-bold uppercase tracking-wider mb-2 block">
            标签
          </label>
          <div className="flex flex-wrap gap-2">
            {metadata.tags.map((tag) => (
              <TagChip key={tag} label={tag} />
            ))}
          </div>
        </div>
      </div>
    </Card>
  )
}

interface VideoConfirmationProps {
  videoId: string
  onVideoLoaded: (video: VideoDetail) => void
}

function VideoConfirmation({ videoId, onVideoLoaded }: VideoConfirmationProps) {
  const { data: video, isLoading, error } = useVideoDetail(videoId)

  useEffect(() => {
    if (video) {
      onVideoLoaded(video)
    }
  }, [video, onVideoLoaded])

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto">
        <h2 className="font-headline text-2xl font-bold mb-6 text-on-surface">
          确认视频信息
        </h2>
        <div className="bg-surface-container-lowest rounded-lg p-12 text-center">
          <Icon name="hourglass_top" size={48} className="text-on-surface-variant animate-spin" />
          <p className="text-on-surface-variant mt-4">加载视频信息...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="max-w-2xl mx-auto">
        <h2 className="font-headline text-2xl font-bold mb-6 text-on-surface">
          确认视频信息
        </h2>
        <div className="bg-error-container/10 rounded-lg p-8 text-center">
          <Icon name="error" size={48} className="text-error" />
          <p className="text-error mt-4">
            {error.message || '加载视频信息失败，请稍后重试'}
          </p>
        </div>
      </div>
    )
  }

  if (!video) {
    return (
      <div className="max-w-2xl mx-auto">
        <h2 className="font-headline text-2xl font-bold mb-6 text-on-surface">
          确认视频信息
        </h2>
        <div className="bg-surface-container-lowest rounded-lg p-8 text-center">
          <Icon name="videocam_off" size={48} className="text-on-surface-variant" />
          <p className="text-on-surface-variant mt-4">视频不存在</p>
        </div>
      </div>
    )
  }

  if (!video.metadata) {
    return (
      <div className="max-w-2xl mx-auto">
        <h2 className="font-headline text-2xl font-bold mb-6 text-on-surface">
          确认视频信息
        </h2>
        <div className="bg-surface-container-lowest rounded-lg p-8 text-center">
          <Icon name="edit_note" size={48} className="text-on-surface-variant" />
          <p className="text-on-surface-variant mt-4">
            视频元数据尚未生成，请先完成元数据审核
          </p>
          <Button
            variant="primary"
            className="mt-4"
            onClick={() => window.location.href = `/videos/${videoId}/metadata`}
          >
            前往元数据审核
          </Button>
        </div>
      </div>
    )
  }

  if (video.status !== 'READY_TO_PUBLISH') {
    return (
      <div className="max-w-2xl mx-auto">
        <h2 className="font-headline text-2xl font-bold mb-6 text-on-surface">
          确认视频信息
        </h2>
        <div className="bg-surface-container-lowest rounded-lg p-8 text-center">
          <Icon name="schedule" size={48} className="text-on-surface-variant" />
          <p className="text-on-surface-variant mt-4">
            视频状态为 {video.status}，暂不能发布
          </p>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto">
      <h2 className="font-headline text-2xl font-bold mb-6 text-on-surface">
        确认视频信息
      </h2>
      <VideoPreviewCard video={video} />
      <MetadataSummaryCard metadata={video.metadata} />
    </div>
  )
}

interface PlatformCardProps {
  platform: DistributionPlatform
  selected: boolean
  disabled: boolean
  onClick: () => void
}

function PlatformCard({ platform, selected, disabled, onClick }: PlatformCardProps) {
  const platformIcons: Record<string, string> = {
    youtube: 'video_library',
    douyin: 'music_video',
    bilibili: 'smart_display',
    xiaohongshu: 'photo_library',
  }

  const platformColors: Record<string, string> = {
    youtube: 'bg-red-50 text-red-600',
    douyin: 'bg-slate-200 text-slate-600',
    bilibili: 'bg-slate-200 text-slate-600',
    xiaohongshu: 'bg-slate-200 text-slate-600',
  }

  const icon = platformIcons[platform.platform] || 'play_circle'
  const colorClass = platformColors[platform.platform] || 'bg-surface-container-high text-on-surface-variant'

  if (disabled || !platform.enabled) {
    return (
      <div className="bg-surface-container-low p-6 rounded-xl border-2 border-transparent opacity-60 grayscale cursor-not-allowed">
        <div className={`w-14 h-14 rounded-lg flex items-center justify-center mb-4 ${colorClass}`}>
          <Icon name={icon} size={28} />
        </div>
        <h3 className="font-bold text-lg mb-1 text-slate-500">{platform.displayName}</h3>
        <p className="text-xs text-slate-400">即将支持</p>
      </div>
    )
  }

  if (!platform.authorized || platform.authExpired) {
    return (
      <div className="bg-surface-container-lowest p-6 rounded-xl border-2 border-error/30 cursor-pointer hover:bg-surface-container-low transition-all">
        <div className={`w-14 h-14 rounded-lg flex items-center justify-center mb-4 ${colorClass}`}>
          <Icon name={icon} size={28} />
        </div>
        <h3 className="font-bold text-lg mb-1">{platform.displayName}</h3>
        <p className="text-xs text-error flex items-center gap-1">
          <Icon name="link_off" size={14} />
          {platform.authExpired ? '授权已过期' : '未授权'}
        </p>
      </div>
    )
  }

  return (
    <div
      onClick={onClick}
      className={`relative bg-surface-container-lowest p-6 rounded-xl cursor-pointer transition-all hover:scale-[1.02] ${
        selected
          ? 'border-2 border-primary shadow-[0_8px_32px_rgba(0,87,194,0.04)]'
          : 'border-2 border-transparent hover:bg-surface-container-low'
      }`}
    >
      {selected && (
        <div className="absolute top-4 right-4 text-primary">
          <Icon name="check_circle" size={24} />
        </div>
      )}
      <div className={`w-14 h-14 rounded-lg flex items-center justify-center mb-4 ${colorClass}`}>
        <Icon name={icon} size={28} />
      </div>
      <h3 className="font-bold text-lg mb-1">{platform.displayName}</h3>
      <p className="text-xs text-green-600 flex items-center gap-1">
        <span className="w-1.5 h-1.5 bg-green-500 rounded-full" />
        OAuth 已连接
      </p>
    </div>
  )
}

interface PlatformSelectionProps {
  selectedPlatform: string
  privacyStatus: 'public' | 'unlisted' | 'private'
  onPlatformChange: (platform: string) => void
  onPrivacyChange: (status: 'public' | 'unlisted' | 'private') => void
}

function PlatformSelection({
  selectedPlatform,
  privacyStatus,
  onPlatformChange,
  onPrivacyChange,
}: PlatformSelectionProps) {
  const { data: platforms, isLoading, error } = usePlatforms()

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto">
        <h2 className="font-headline text-2xl font-bold mb-6 text-on-surface">
          选择发布平台
        </h2>
        <div className="bg-surface-container-lowest rounded-lg p-12 text-center">
          <Icon name="hourglass_top" size={48} className="text-on-surface-variant animate-spin" />
          <p className="text-on-surface-variant mt-4">加载平台列表...</p>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="max-w-4xl mx-auto">
        <h2 className="font-headline text-2xl font-bold mb-6 text-on-surface">
          选择发布平台
        </h2>
        <div className="bg-error-container/10 rounded-lg p-8 text-center">
          <Icon name="error" size={48} className="text-error" />
          <p className="text-error mt-4">加载平台列表失败</p>
        </div>
      </div>
    )
  }

  const privacyOptions = [
    { value: 'public', label: '公开' },
    { value: 'unlisted', label: '不公开列出' },
    { value: 'private', label: '私有' },
  ]

  return (
    <div className="max-w-4xl mx-auto">
      <h2 className="font-headline text-2xl font-bold mb-6 text-on-surface">
        选择发布平台
      </h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {(platforms ?? []).map((platform) => (
          <PlatformCard
            key={platform.platform}
            platform={platform}
            selected={selectedPlatform === platform.platform}
            disabled={!platform.enabled}
            onClick={() => {
              if (platform.authorized && !platform.authExpired && platform.enabled) {
                onPlatformChange(platform.platform)
              }
            }}
          />
        ))}
      </div>
      {selectedPlatform && (
        <div className="bg-surface-container-lowest rounded-lg p-6">
          <h3 className="font-body text-sm font-bold text-on-surface mb-4">
            隐私设置
          </h3>
          <Select
            options={privacyOptions}
            value={privacyStatus}
            onChange={(value) => onPrivacyChange(value as 'public' | 'unlisted' | 'private')}
          />
        </div>
      )}
    </div>
  )
}

interface EditablePromotionCopy {
  channelId: string
  channelName: string
  channelType: string
  promotionTitle: string
  promotionBody: string
  recommendedMethod: string
}

interface PromotionCopyCardProps {
  copy: EditablePromotionCopy
  onTitleChange: (title: string) => void
  onBodyChange: (body: string) => void
  onRegenerate: () => void
  isRegenerating: boolean
}

function PromotionCopyCard({
  copy,
  onTitleChange,
  onBodyChange,
  onRegenerate,
  isRegenerating,
}: PromotionCopyCardProps) {
  const channelIcons: Record<string, string> = {
    weibo: 'campaign',
    reddit: 'forum',
    douban: 'book',
    twitter: 'tag',
    facebook: 'share',
    instagram: 'photo_camera',
  }

  const channelColors: Record<string, string> = {
    weibo: 'bg-red-100 text-red-500',
    reddit: 'bg-orange-100 text-orange-600',
    douban: 'bg-green-100 text-green-700',
    twitter: 'bg-blue-100 text-blue-500',
    facebook: 'bg-blue-100 text-blue-600',
    instagram: 'bg-purple-100 text-purple-500',
  }

  const icon = channelIcons[copy.channelName.toLowerCase()] || 'share'
  const colorClass = channelColors[copy.channelName.toLowerCase()] || 'bg-surface-container-high text-on-surface-variant'

  return (
    <div className="bg-surface-container-lowest p-6 rounded-xl shadow-[0_8px_32px_rgba(0,26,67,0.04)] flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className={`w-10 h-10 rounded-full flex items-center justify-center ${colorClass}`}>
            <Icon name={icon} size={20} />
          </div>
          <span className="font-bold text-sm">{copy.channelName}</span>
        </div>
        <span className="text-[10px] font-bold text-slate-400 bg-slate-100 px-2 py-0.5 rounded">
          {copy.recommendedMethod}
        </span>
      </div>
      <div>
        <label className="text-[10px] text-slate-400 font-bold mb-1 block uppercase tracking-wider">
          标题
        </label>
        <Input
          value={copy.promotionTitle}
          onChange={(e) => onTitleChange(e.target.value)}
          className="font-medium"
        />
      </div>
      <div>
        <label className="text-[10px] text-slate-400 font-bold mb-1 block uppercase tracking-wider">
          正文
        </label>
        <Textarea
          value={copy.promotionBody}
          onChange={(e) => onBodyChange(e.target.value)}
          rows={5}
          className="leading-relaxed h-32"
        />
      </div>
      <div className="mt-auto flex justify-end">
        <button
          onClick={onRegenerate}
          disabled={isRegenerating}
          className="text-primary text-xs font-bold flex items-center gap-1 hover:underline disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Icon name="refresh" size={14} className={isRegenerating ? 'animate-spin' : ''} />
          {isRegenerating ? '生成中...' : '重新生成'}
        </button>
      </div>
    </div>
  )
}

interface PromotionConfigProps {
  videoId: string
  editableCopies: EditablePromotionCopy[]
  onCopiesChange: (copies: EditablePromotionCopy[]) => void
  onGenerateCopy: () => void
  isGenerating: boolean
}

function PromotionConfig({
  videoId,
  editableCopies,
  onCopiesChange,
  onGenerateCopy,
  isGenerating,
}: PromotionConfigProps) {
  const generateCopyMutation = useGenerateCopy()

  useEffect(() => {
    if (editableCopies.length === 0 && !isGenerating) {
      onGenerateCopy()
    }
  }, [editableCopies.length, isGenerating, onGenerateCopy])

  const handleTitleChange = (channelId: string, title: string) => {
    onCopiesChange(
      editableCopies.map((copy) =>
        copy.channelId === channelId ? { ...copy, promotionTitle: title } : copy
      )
    )
  }

  const handleBodyChange = (channelId: string, body: string) => {
    onCopiesChange(
      editableCopies.map((copy) =>
        copy.channelId === channelId ? { ...copy, promotionBody: body } : copy
      )
    )
  }

  const handleRegenerateSingle = async (channelId: string) => {
    try {
      const result = await generateCopyMutation.mutateAsync({
        videoId,
        channelIds: [channelId],
      })
      const newCopy = result[0]
      if (newCopy) {
        onCopiesChange(
          editableCopies.map((copy) =>
            copy.channelId === channelId
              ? {
                  channelId: newCopy.channelId,
                  channelName: newCopy.channelName,
                  channelType: newCopy.channelType,
                  promotionTitle: newCopy.promotionTitle,
                  promotionBody: newCopy.promotionBody,
                  recommendedMethod: newCopy.recommendedMethod,
                }
              : copy
          )
        )
      }
    } catch {
      // Error handling - could show toast notification
    }
  }

  if (isGenerating || editableCopies.length === 0) {
    return (
      <div className="max-w-6xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <h2 className="font-headline text-2xl font-bold text-on-surface">
            配置推广文案
          </h2>
          <span className="text-xs font-medium text-primary bg-primary/10 px-3 py-1 rounded-full flex items-center gap-1">
            <Icon name="auto_awesome" size={14} />
            AI 生成中
          </span>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-surface-container-lowest p-6 rounded-xl">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-full bg-surface-container-high animate-pulse" />
                <div className="h-4 w-16 bg-surface-container-high animate-pulse rounded" />
              </div>
              <div className="h-4 w-full bg-surface-container-high animate-pulse rounded mb-4" />
              <div className="h-32 w-full bg-surface-container-high animate-pulse rounded" />
            </div>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-6xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h2 className="font-headline text-2xl font-bold text-on-surface">
          AI 生成推广文案
        </h2>
        <span className="text-xs font-medium text-primary bg-primary/10 px-3 py-1 rounded-full flex items-center gap-1">
          <Icon name="auto_awesome" size={14} />
          智能优化完成
        </span>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {editableCopies.map((copy) => (
          <PromotionCopyCard
            key={copy.channelId}
            copy={copy}
            onTitleChange={(title) => handleTitleChange(copy.channelId, title)}
            onBodyChange={(body) => handleBodyChange(copy.channelId, body)}
            onRegenerate={() => handleRegenerateSingle(copy.channelId)}
            isRegenerating={generateCopyMutation.isPending}
          />
        ))}
      </div>
    </div>
  )
}

interface PublishResultViewProps {
  publishStatus: 'uploading' | 'success' | 'failed'
  progress: number
  videoUrl?: string
  errorMessage?: string
  promotionResults?: Array<{
    channelName: string
    status: string
    resultUrl?: string
  }>
}

function PublishResultView({
  publishStatus,
  progress,
  videoUrl,
  errorMessage,
  promotionResults,
}: PublishResultViewProps) {
  if (publishStatus === 'uploading') {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-surface-container-lowest rounded-xl p-8 text-center">
          <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-4">
            <Icon name="cloud_upload" size={32} className="text-primary animate-pulse" />
          </div>
          <h3 className="font-headline text-xl font-bold text-on-surface mb-2">
            正在发布视频...
          </h3>
          <div className="w-full max-w-xs mx-auto mt-4">
            <div className="h-2 bg-surface-container-high rounded-full overflow-hidden">
              <div
                className="h-full bg-primary transition-all duration-500"
                style={{ width: `${progress}%` }}
              />
            </div>
            <p className="text-sm text-on-surface-variant mt-2">{progress}% 完成</p>
          </div>
          {promotionResults && promotionResults.length > 0 && (
            <div className="mt-6 text-sm text-on-surface-variant">
              <p>同时执行推广任务...</p>
            </div>
          )}
        </div>
      </div>
    )
  }

  if (publishStatus === 'failed') {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-error-container/10 rounded-xl p-8 text-center">
          <div className="w-16 h-16 rounded-full bg-error/10 flex items-center justify-center mx-auto mb-4">
            <Icon name="error" size={32} className="text-error" />
          </div>
          <h3 className="font-headline text-xl font-bold text-error mb-2">
            发布失败
          </h3>
          <p className="text-sm text-on-surface-variant mt-2">{errorMessage}</p>
          <Button variant="secondary" className="mt-4">
            重试
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-surface-container-lowest rounded-xl p-8">
        <div className="text-center mb-8">
          <div className="w-16 h-16 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-4">
            <Icon name="check_circle" size={32} className="text-green-600" />
          </div>
          <h3 className="font-headline text-xl font-bold text-on-surface mb-2">
            发布成功！
          </h3>
          {videoUrl && (
            <a
              href={videoUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-sm text-primary hover:underline flex items-center justify-center gap-1"
            >
              <Icon name="link" size={16} />
              查看已发布视频
            </a>
          )}
        </div>
        {promotionResults && promotionResults.length > 0 && (
          <div className="border-t border-surface-container-high pt-6">
            <h4 className="font-body text-sm font-bold text-on-surface mb-4">
              推广任务结果
            </h4>
            <div className="space-y-3">
              {promotionResults.map((result, index) => (
                <div
                  key={index}
                  className="flex items-center justify-between bg-surface-container-low rounded-lg p-3"
                >
                  <span className="text-sm font-medium">{result.channelName}</span>
                  <div className="flex items-center gap-2">
                    <span
                      className={`text-xs px-2 py-0.5 rounded ${
                        result.status === 'COMPLETED'
                          ? 'bg-green-100 text-green-600'
                          : 'bg-error-container/10 text-error'
                      }`}
                    >
                      {result.status === 'COMPLETED' ? '成功' : '失败'}
                    </span>
                    {result.resultUrl && (
                      <a
                        href={result.resultUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-primary text-xs hover:underline"
                      >
                        查看
                      </a>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default function DistributionPromotionPage() {
  const { videoId } = useParams<{ videoId: string }>()
  const navigate = useNavigate()
  const [currentStep, setCurrentStep] = useState<StepId>(1)
  const [videoData, setVideoData] = useState<VideoDetail | null>(null)
  const [selectedPlatform, setSelectedPlatform] = useState<string>('youtube')
  const [privacyStatus, setPrivacyStatus] = useState<'public' | 'unlisted' | 'private'>('public')
  const [editableCopies, setEditableCopies] = useState<EditablePromotionCopy[]>([])
  const [isGeneratingCopy, setIsGeneratingCopy] = useState(false)
  const [publishState, setPublishState] = useState<{
    status: 'idle' | 'uploading' | 'success' | 'failed'
    taskId: string
    progress: number
    videoUrl: string | undefined
    errorMessage: string | undefined
    promotionResults: Array<{
      channelName: string
      status: string
      resultUrl?: string
    }> | undefined
  }>({
    status: 'idle',
    taskId: '',
    progress: 0,
    videoUrl: undefined,
    errorMessage: undefined,
    promotionResults: undefined,
  })

  const publishMutation = usePublish()
  const executePromotionMutation = useExecutePromotion()
  const generateCopyMutation = useGenerateCopy()
  const { data: publishStatusData } = usePublishStatus(publishState.taskId)

  useEffect(() => {
    if (publishState.taskId && publishStatusData) {
      setPublishState((prev) => ({
        ...prev,
        progress: publishStatusData.progressPercent,
        videoUrl: publishStatusData.videoUrl ?? undefined,
        errorMessage: publishStatusData.errorMessage ?? undefined,
      }))

      if (publishStatusData.status === 'COMPLETED') {
        setPublishState((prev) => ({
          ...prev,
          status: 'success',
        }))
      } else if (publishStatusData.status === 'FAILED') {
        setPublishState((prev) => ({
          ...prev,
          status: 'failed',
        }))
      }
    }
  }, [publishState.taskId, publishStatusData])

  const handleVideoLoaded = useCallback((video: VideoDetail) => {
    setVideoData(video)
  }, [])

  const handleGenerateCopy = useCallback(async () => {
    if (!videoId) return
    setIsGeneratingCopy(true)
    try {
      const result = await generateCopyMutation.mutateAsync({ videoId })
      setEditableCopies(
        result.map((copy) => ({
          channelId: copy.channelId,
          channelName: copy.channelName,
          channelType: copy.channelType,
          promotionTitle: copy.promotionTitle,
          promotionBody: copy.promotionBody,
          recommendedMethod: copy.recommendedMethod,
        }))
      )
    } catch {
      // Fallback: allow manual editing if AI fails
      setEditableCopies([
        {
          channelId: 'default-1',
          channelName: 'Weibo',
          channelType: 'SOCIAL_MEDIA',
          promotionTitle: '',
          promotionBody: '',
          recommendedMethod: 'POST',
        },
      ])
    } finally {
      setIsGeneratingCopy(false)
    }
  }, [videoId, generateCopyMutation])

  const handleBack = () => {
    if (currentStep > 1) {
      setCurrentStep((prev) => (prev - 1) as StepId)
    }
  }

  const handleNext = async () => {
    if (currentStep < 3) {
      setCurrentStep((prev) => (prev + 1) as StepId)
    } else {
      if (!videoData?.metadata || !videoId) return

      setPublishState((prev) => ({ ...prev, status: 'uploading', progress: 10 }))

      try {
        const publishResult = await publishMutation.mutateAsync({
          videoId,
          metadataId: videoData.metadata.metadataId,
          platform: selectedPlatform,
          privacyStatus,
        })

        setPublishState((prev) => ({
          ...prev,
          taskId: publishResult.uploadTaskId,
          progress: 20,
        }))

        const promotionItems: PromotionItem[] = editableCopies.map((copy) => ({
          channelId: copy.channelId,
          promotionTitle: copy.promotionTitle,
          promotionBody: copy.promotionBody,
          method: copy.recommendedMethod as 'POST' | 'COMMENT' | 'SHARE',
        }))

        if (promotionItems.length > 0) {
          const promotionResults = await executePromotionMutation.mutateAsync({
            videoId,
            promotionItems,
          })

          setPublishState((prev) => ({
            ...prev,
            promotionResults: promotionResults.map((r) => ({
              channelName: r.channelName,
              status: r.status,
              resultUrl: r.resultUrl ?? undefined,
            })),
          }))
        }
      } catch (err) {
        setPublishState((prev) => ({
          ...prev,
          status: 'failed',
          errorMessage: err instanceof Error ? err.message : '发布失败',
        }))
      }
    }
  }

  const handleGoToDashboard = () => {
    navigate('/dashboard')
  }

  if (publishState.status !== 'idle') {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="font-headline text-2xl font-bold text-on-surface">
              分发与推广
            </h1>
            <p className="font-body text-sm text-on-surface-variant mt-1">
              发布进度
            </p>
          </div>
        </div>
        <div className="bg-surface-container-lowest rounded-xl p-8">
          <PublishResultView
            publishStatus={publishState.status}
            progress={publishState.progress}
            videoUrl={publishState.videoUrl}
            errorMessage={publishState.errorMessage}
            promotionResults={publishState.promotionResults}
          />
          {publishState.status === 'success' && (
            <footer className="flex items-center justify-center mt-8 pt-8 border-t border-surface-container-high">
              <Button variant="primary" onClick={handleGoToDashboard}>
                返回仪表盘
              </Button>
            </footer>
          )}
        </div>
      </div>
    )
  }

  const renderStepContent = () => {
    switch (currentStep) {
      case 1:
        return (
          <VideoConfirmation
            videoId={videoId ?? ''}
            onVideoLoaded={handleVideoLoaded}
          />
        )
      case 2:
        return (
          <PlatformSelection
            selectedPlatform={selectedPlatform}
            privacyStatus={privacyStatus}
            onPlatformChange={setSelectedPlatform}
            onPrivacyChange={setPrivacyStatus}
          />
        )
      case 3:
        return (
          <PromotionConfig
            videoId={videoId ?? ''}
            editableCopies={editableCopies}
            onCopiesChange={setEditableCopies}
            onGenerateCopy={handleGenerateCopy}
            isGenerating={isGeneratingCopy}
          />
        )
      default:
        return null
    }
  }

  const canProceed = () => {
    if (currentStep === 1) {
      return videoData?.metadata && videoData?.status === 'READY_TO_PUBLISH'
    }
    if (currentStep === 2) {
      return selectedPlatform
    }
    if (currentStep === 3) {
      return editableCopies.length > 0 && editableCopies.every((c) => c.promotionTitle && c.promotionBody)
    }
    return true
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-headline text-2xl font-bold text-on-surface">
            分发与推广
          </h1>
          <p className="font-body text-sm text-on-surface-variant mt-1">
            一键发布视频到多平台并执行推广
          </p>
        </div>
      </div>

      <div className="bg-surface-container-lowest rounded-xl p-8">
        <StepIndicator steps={STEPS} currentStep={currentStep} />
        <div className="min-h-[300px]">{renderStepContent()}</div>
        <footer className="flex items-center justify-between mt-8 pt-8 border-t border-surface-container-high">
          <button
            onClick={handleBack}
            disabled={currentStep === 1}
            className="px-8 py-3 rounded-lg text-on-surface-variant font-bold flex items-center gap-2 hover:bg-surface-container-low transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Icon name="arrow_back" size={18} />
            返回
          </button>

          {currentStep === 3 ? (
            <button
              onClick={handleNext}
              disabled={!canProceed() || publishMutation.isPending}
              className="px-12 py-3.5 rounded-lg bg-gradient-to-r from-primary to-primary-container text-white font-bold flex items-center gap-3 shadow-lg shadow-primary/30 hover:shadow-xl hover:shadow-primary/40 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {publishMutation.isPending ? '发布中...' : '确认发布'}
              <Icon name="rocket_launch" size={18} />
            </button>
          ) : (
            <button
              onClick={handleNext}
              disabled={!canProceed()}
              className="px-8 py-3 rounded-lg bg-primary text-white font-bold flex items-center gap-2 hover:opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              下一步
              <Icon name="arrow_forward" size={18} />
            </button>
          )}
        </footer>
      </div>
    </div>
  )
}