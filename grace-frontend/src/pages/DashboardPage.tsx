import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDashboardOverview } from '@/hooks/useDashboard'
import { Icon } from '@/components/ui/Icon'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { ProgressBar } from '@/components/ui/ProgressBar'
import type { DateRange, RecentUpload, PromotionOverviewItem, PublishDistribution } from '@/types/dashboard'

interface StatsCardProps {
  label: string
  value: number
  icon: string
  borderColor: 'primary' | 'orange' | 'green' | 'tertiary'
}

const borderColorMap = {
  primary: 'border-primary',
  orange: 'border-orange-400',
  green: 'border-green-500',
  tertiary: 'border-tertiary',
}

const iconColorMap = {
  primary: 'text-primary',
  orange: 'text-orange-500',
  green: 'text-green-500',
  tertiary: 'text-tertiary',
}

function StatsCard({ label, value, icon, borderColor }: StatsCardProps) {
  return (
    <div
      className={`bg-surface-container-lowest rounded-lg p-6 h-32 flex flex-col justify-between border-l-4 ${borderColorMap[borderColor]}`}
    >
      <div className="flex justify-between items-start">
        <span className="font-body text-sm font-medium text-on-surface-variant">
          {label}
        </span>
        <Icon name={icon} size={20} className={iconColorMap[borderColor]} />
      </div>
      <div className="font-headline text-3xl font-bold text-on-surface">
        {value}
      </div>
    </div>
  )
}

interface StatsCardGridProps {
  stats: {
    totalVideos: number
    pendingReview: number
    published: number
    promoting: number
  }
}

function StatsCardGrid({ stats }: StatsCardGridProps) {
  const cards = [
    {
      label: '总视频数',
      value: stats.totalVideos,
      icon: 'video_library',
      borderColor: 'primary' as const,
    },
    {
      label: '待审核',
      value: stats.pendingReview,
      icon: 'schedule',
      borderColor: 'orange' as const,
    },
    {
      label: '已发布',
      value: stats.published,
      icon: 'check_circle',
      borderColor: 'green' as const,
    },
    {
      label: '推广中',
      value: stats.promoting,
      icon: 'rocket_launch',
      borderColor: 'tertiary' as const,
    },
  ]

  return (
    <div className="grid grid-cols-4 gap-6">
      {cards.map((card) => (
        <StatsCard
          key={card.label}
          label={card.label}
          value={card.value}
          icon={card.icon}
          borderColor={card.borderColor}
        />
      ))}
    </div>
  )
}

interface RecentUploadsTableProps {
  uploads: RecentUpload[]
}

function RecentUploadsTable({ uploads }: RecentUploadsTableProps) {
  const navigate = useNavigate()

  const handleRowClick = (videoId: string) => {
    navigate(`/videos/${videoId}/metadata`)
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left border-collapse">
        <thead>
          <tr className="text-on-surface-variant border-b border-surface-container-high">
            <th className="pb-4 font-semibold text-sm">视频预览</th>
            <th className="pb-4 font-semibold text-sm">文件名</th>
            <th className="pb-4 font-semibold text-sm">上传日期</th>
            <th className="pb-4 font-semibold text-sm">状态</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-surface-container-low">
          {uploads.map((upload) => (
            <tr
              key={upload.videoId}
              onClick={() => handleRowClick(upload.videoId)}
              className="group hover:bg-surface-bright transition-colors cursor-pointer"
            >
              <td className="py-4">
                <div className="w-20 h-12 rounded bg-surface-container-low overflow-hidden">
                  {upload.thumbnailUrl ? (
                    <img
                      src={upload.thumbnailUrl}
                      alt={upload.fileName}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full bg-surface-container-high flex items-center justify-center">
                      <Icon name="video_file" size={16} className="text-on-surface-variant" />
                    </div>
                  )}
                </div>
              </td>
              <td className="py-4 font-body text-sm font-medium text-on-surface">
                {upload.fileName}
              </td>
              <td className="py-4 font-body text-xs text-on-surface-variant">
                {new Date(upload.createdAt).toLocaleDateString('zh-CN')}
              </td>
              <td className="py-4">
                <StatusBadge status={upload.status as Parameters<typeof StatusBadge>[0]['status']} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

interface DonutChartProps {
  data: PublishDistribution
}

function DonutChart({ data }: DonutChartProps) {
  const total = data.published + data.pending + data.failed

  if (total === 0) {
    return (
      <div className="flex items-center justify-center h-48 text-on-surface-variant">
        暂无数据
      </div>
    )
  }

  // Calculate percentages and stroke dash arrays
  const publishedPercent = (data.published / total) * 100
  const pendingPercent = (data.pending / total) * 100
  const failedPercent = (data.failed / total) * 100

  return (
    <div className="relative flex-grow flex items-center justify-center">
      <svg className="w-48 h-48" viewBox="0 0 36 36">
        {/* Background ring */}
        <path
          className="text-surface-container-high"
          d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
          fill="none"
          stroke="currentColor"
          strokeDasharray="100, 100"
          strokeWidth="3"
        />
        {/* Published (Green) */}
        <path
          className="text-green-500"
          d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
          fill="none"
          stroke="currentColor"
          strokeDasharray={`${publishedPercent}, 100`}
          strokeLinecap="round"
          strokeWidth="3"
        />
        {/* Pending (Orange) */}
        <path
          className="text-orange-400"
          d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
          fill="none"
          stroke="currentColor"
          strokeDasharray={`${pendingPercent}, 100`}
          strokeDashoffset={-publishedPercent}
          strokeLinecap="round"
          strokeWidth="3"
        />
        {/* Failed (Red) */}
        <path
          className="text-error"
          d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
          fill="none"
          stroke="currentColor"
          strokeDasharray={`${failedPercent}, 100`}
          strokeDashoffset={-(publishedPercent + pendingPercent)}
          strokeLinecap="round"
          strokeWidth="3"
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-3xl font-extrabold font-headline">{total}</span>
        <span className="text-xs text-on-surface-variant font-medium">总任务</span>
      </div>
    </div>
  )
}

interface PromotionOverviewProps {
  channels: PromotionOverviewItem[]
}

function PromotionOverview({ channels }: PromotionOverviewProps) {
  const getChannelIcon = (channelName: string): string => {
    const name = channelName.toLowerCase()
    if (name.includes('weibo') || name.includes('微博')) return 'share'
    if (name.includes('reddit')) return 'forum'
    if (name.includes('douban') || name.includes('豆瓣')) return 'reviews'
    return 'send'
  }

  const getChannelColor = (channelName: string): { bg: string; icon: string } => {
    const name = channelName.toLowerCase()
    if (name.includes('weibo') || name.includes('微博')) return { bg: 'bg-red-100', icon: 'text-red-600' }
    if (name.includes('reddit')) return { bg: 'bg-orange-100', icon: 'text-orange-600' }
    if (name.includes('douban') || name.includes('豆瓣')) return { bg: 'bg-green-100', icon: 'text-green-600' }
    return { bg: 'bg-surface-container-high', icon: 'text-primary' }
  }

  return (
    <div className="space-y-8">
      {channels.map((channel) => {
        const colors = getChannelColor(channel.channelName)
        return (
          <div key={channel.channelId}>
            <div className="flex justify-between items-center mb-2">
              <div className="flex items-center gap-3">
                <div className={`w-8 h-8 rounded-full ${colors.bg} flex items-center justify-center`}>
                  <Icon name={getChannelIcon(channel.channelName)} size={16} className={colors.icon} />
                </div>
                <span className="font-body text-sm font-medium">{channel.channelName}</span>
              </div>
              <span className="font-body text-sm font-medium text-primary">
                {Math.round(channel.successRate * 100)}%
              </span>
            </div>
            <ProgressBar progress={channel.successRate * 100} />
          </div>
        )
      })}
    </div>
  )
}

interface AnalyticsProps {
  avgEngagementRate: number
  totalImpressions: number
}

function Analytics({ avgEngagementRate, totalImpressions }: AnalyticsProps) {
  const formatImpressions = (num: number): string => {
    if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`
    if (num >= 1000) return `${(num / 1000).toFixed(1)}k`
    return num.toString()
  }

  return (
    <div className="mt-12 p-6 bg-surface-container-low rounded-lg flex items-center justify-between">
      <div className="flex items-center gap-6">
        <div>
          <div className="text-[10px] text-on-surface-variant font-bold tracking-wider mb-1">
            平均互动率
          </div>
          <div className="text-2xl font-extrabold font-headline">{avgEngagementRate.toFixed(1)}%</div>
        </div>
        <div className="h-10 w-px bg-outline-variant opacity-20" />
        <div>
          <div className="text-[10px] text-on-surface-variant font-bold tracking-wider mb-1">
            总曝光量
          </div>
          <div className="text-2xl font-extrabold font-headline">{formatImpressions(totalImpressions)}</div>
        </div>
      </div>
      <button className="flex items-center gap-2 bg-surface-container-lowest py-3 px-6 rounded-lg text-sm font-bold shadow-sm hover:shadow-md transition-all active:scale-95">
        <Icon name="download" size={18} className="text-primary" />
        导出完整报告
      </button>
    </div>
  )
}

export default function DashboardPage() {
  const [dateRange, setDateRange] = useState<DateRange>('30d')
  const { data, isLoading } = useDashboardOverview(dateRange)
  const navigate = useNavigate()

  const stats = data?.data?.stats ?? {
    totalVideos: 0,
    pendingReview: 0,
    published: 0,
    promoting: 0,
  }

  const recentUploads = data?.data?.recentUploads ?? []
  const publishDistribution = data?.data?.publishDistribution ?? { published: 0, pending: 0, failed: 0 }
  const promotionOverview = data?.data?.promotionOverview ?? []
  const analytics = data?.data?.analytics ?? { avgEngagementRate: 0, totalImpressions: 0 }

  return (
    <div className="px-8 py-6">
      {/* Header Section */}
      <div className="mb-10">
        <div className="flex justify-between items-start">
          <div>
            <h1 className="font-headline text-4xl font-extrabold tracking-tight text-on-surface mb-2">
              数据概览
            </h1>
            <p className="font-body text-on-surface-variant">
              欢迎回来，您的视频分发任务运行正常。
            </p>
          </div>
          <div className="flex gap-2">
            {(['7d', '30d', '90d', 'all'] as DateRange[]).map((range) => (
              <button
                key={range}
                onClick={() => setDateRange(range)}
                className={`px-4 py-2 text-xs font-semibold rounded transition-colors ${
                  dateRange === range
                    ? 'bg-primary text-white shadow-sm'
                    : 'bg-surface-container-low text-on-surface hover:bg-surface-container-high'
                }`}
              >
                {range === '7d' && '近7天'}
                {range === '30d' && '近30天'}
                {range === '90d' && '近90天'}
                {range === 'all' && '全部'}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Stats Cards Grid */}
      {isLoading ? (
        <div className="grid grid-cols-4 gap-6">
          {[...Array(4)].map((_, i) => (
            <div
              key={i}
              className="bg-surface-container-lowest rounded-lg p-6 h-32 border-l-4 border-outline-variant animate-pulse"
            />
          ))}
        </div>
      ) : (
        <StatsCardGrid stats={stats} />
      )}

      {/* Middle Row: Recent Uploads & Publish Stats */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-8 mt-10">
        {/* Left (60%): Recent Uploads Table */}
        <div className="lg:col-span-3 bg-surface-container-lowest p-8 rounded-lg shadow-[0_8px_32px_rgba(0,26,67,0.04)]">
          <div className="flex justify-between items-center mb-8">
            <h2 className="text-xl font-bold font-headline">最近上传</h2>
            <button
              onClick={() => navigate('/videos')}
              className="text-primary text-sm font-semibold hover:underline"
            >
              查看全部
            </button>
          </div>
          {isLoading ? (
            <div className="space-y-4">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="h-16 bg-surface-container-high rounded animate-pulse" />
              ))}
            </div>
          ) : recentUploads.length > 0 ? (
            <RecentUploadsTable uploads={recentUploads} />
          ) : (
            <div className="text-center py-12 text-on-surface-variant">
              暂无最近上传
            </div>
          )}
        </div>

        {/* Right (40%): Publish Stats Donut Chart */}
        <div className="lg:col-span-2 bg-surface-container-lowest p-8 rounded-lg shadow-[0_8px_32px_rgba(0,26,67,0.04)] flex flex-col">
          <h2 className="text-xl font-bold font-headline mb-8">发布统计</h2>
          {isLoading ? (
            <div className="flex-grow flex items-center justify-center">
              <div className="w-48 h-48 rounded-full bg-surface-container-high animate-pulse" />
            </div>
          ) : (
            <>
              <DonutChart data={publishDistribution} />
              <div className="mt-8 grid grid-cols-3 gap-2">
                <div className="flex flex-col items-center">
                  <div className="flex items-center gap-1.5 mb-1">
                    <span className="w-2 h-2 rounded-full bg-green-500" />
                    <span className="text-[10px] text-on-surface-variant uppercase font-bold tracking-wider">
                      已发布
                    </span>
                  </div>
                  <span className="text-lg font-bold">{publishDistribution.published}</span>
                </div>
                <div className="flex flex-col items-center">
                  <div className="flex items-center gap-1.5 mb-1">
                    <span className="w-2 h-2 rounded-full bg-orange-400" />
                    <span className="text-[10px] text-on-surface-variant uppercase font-bold tracking-wider">
                      处理中
                    </span>
                  </div>
                  <span className="text-lg font-bold">{publishDistribution.pending}</span>
                </div>
                <div className="flex flex-col items-center">
                  <div className="flex items-center gap-1.5 mb-1">
                    <span className="w-2 h-2 rounded-full bg-error" />
                    <span className="text-[10px] text-on-surface-variant uppercase font-bold tracking-wider">
                      失败
                    </span>
                  </div>
                  <span className="text-lg font-bold">{publishDistribution.failed}</span>
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Bottom Row: Promotion Overview */}
      <div className="bg-surface-container-lowest p-8 rounded-lg shadow-[0_8px_32px_rgba(0,26,67,0.04)] mt-10">
        <div className="flex justify-between items-center mb-10">
          <div>
            <h2 className="text-xl font-bold font-headline">推广执行概览</h2>
            <p className="text-sm text-on-surface-variant mt-1">分渠道推广成功率及流量表现</p>
          </div>
        </div>
        {isLoading ? (
          <div className="space-y-8">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="space-y-2">
                <div className="h-4 w-32 bg-surface-container-high rounded animate-pulse" />
                <div className="h-3 w-full bg-surface-container-high rounded animate-pulse" />
              </div>
            ))}
          </div>
        ) : promotionOverview.length > 0 ? (
          <PromotionOverview channels={promotionOverview} />
        ) : (
          <div className="text-center py-12 text-on-surface-variant">
            暂无推广数据
          </div>
        )}
        <Analytics
          avgEngagementRate={analytics.avgEngagementRate}
          totalImpressions={analytics.totalImpressions}
        />
      </div>
    </div>
  )
}
