import { useState } from 'react'
import { useDashboardOverview } from '@/hooks/useDashboard'
import { Icon } from '@/components/ui/Icon'
import type { DateRange } from '@/types/dashboard'

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

export default function DashboardPage() {
  const [dateRange, setDateRange] = useState<DateRange>('30d')
  const { data, isLoading } = useDashboardOverview(dateRange)

  const stats = data?.data?.stats ?? {
    totalVideos: 0,
    pendingReview: 0,
    published: 0,
    promoting: 0,
  }

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
    </div>
  )
}
