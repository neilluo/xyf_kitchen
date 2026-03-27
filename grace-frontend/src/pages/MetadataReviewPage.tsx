import { useParams } from 'react-router-dom'
import { useVideoDetail } from '@/hooks/useVideos'
import { Icon } from '@/components/ui/Icon'
import { formatFileSize } from '@/utils/format'

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
  const { data: video, isLoading, error } = useVideoDetail(videoId || '')

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
        <div>
          <h1 className="font-headline text-2xl font-bold text-on-surface">
            元数据审核
          </h1>
          <p className="font-body text-sm text-on-surface-variant mt-1">
            审核并编辑 AI 生成的视频元数据
          </p>
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

        {/* Right Column: Metadata Editor (Placeholder for P9-07) */}
        <section className="bg-surface-container-lowest rounded-lg p-6 min-h-[400px] flex items-center justify-center">
          <div className="text-center text-on-surface-variant">
            <Icon name="auto_awesome" size={32} className="mx-auto mb-3" />
            <p className="font-body text-sm">元数据编辑器即将推出</p>
          </div>
        </section>
      </div>
    </div>
  )
}
