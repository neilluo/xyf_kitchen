import { useState, useCallback, useMemo } from 'react'
import { useVideoList } from '@/hooks/useVideos'
import { usePromotionHistory, usePromotionReport, useRetryPromotion } from '@/hooks/usePromotions'
import { Button } from '@/components/ui/Button'
import { Icon } from '@/components/ui/Icon'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { Pagination } from '@/components/ui/Pagination'
import type { PromotionRecord, PromotionMethod, PromotionStatus, ChannelExecutionSummary } from '@/types/promotion'
import type { Video } from '@/types/video'
import { formatDate } from '@/utils/format'

const CHANNEL_COLORS: Record<string, { bg: string; text: string; abbr: string }> = {
  YouTube: { bg: 'bg-[#FF0000]', text: 'text-white', abbr: 'YT' },
  TikTok: { bg: 'bg-[#FE2C55]', text: 'text-white', abbr: 'TK' },
  Facebook: { bg: 'bg-[#1877F2]', text: 'text-white', abbr: 'FB' },
  Instagram: { bg: 'bg-[#E4405F]', text: 'text-white', abbr: 'IG' },
  Weibo: { bg: 'bg-[#E6162D]', text: 'text-white', abbr: 'WB' },
  Reddit: { bg: 'bg-[#FF4500]', text: 'text-white', abbr: 'RD' },
}

function getChannelConfig(channelName: string) {
  return CHANNEL_COLORS[channelName] || { bg: 'bg-surface-container-high', text: 'text-on-surface', abbr: channelName.slice(0, 2).toUpperCase() }
}

interface MethodBadgeProps {
  method: PromotionMethod
}

function MethodBadge({ method }: MethodBadgeProps) {
  return (
    <span className="px-2 py-1 bg-surface-container-high rounded text-[10px] font-bold text-on-surface-variant uppercase tracking-tight">
      {method}
    </span>
  )
}

interface CircularProgressProps {
  progress: number
  size?: number
  strokeWidth?: number
}

function CircularProgress({ progress, size = 48, strokeWidth = 4 }: CircularProgressProps) {
  const radius = (size - strokeWidth) / 2
  const circumference = 2 * Math.PI * radius
  const offset = circumference - (progress / 100) * circumference

  return (
    <div className="relative" style={{ width: size, height: size }}>
      <svg className="w-full h-full -rotate-90" viewBox={`0 0 ${size} ${size}`}>
        <circle
          className="text-surface-container-highest"
          cx={size / 2}
          cy={size / 2}
          fill="transparent"
          r={radius}
          stroke="currentColor"
          strokeWidth={strokeWidth}
        />
        <circle
          className="text-primary transition-all duration-500"
          cx={size / 2}
          cy={size / 2}
          fill="transparent"
          r={radius}
          stroke="currentColor"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeWidth={strokeWidth}
          strokeLinecap="round"
        />
      </svg>
      <div className="absolute inset-0 flex items-center justify-center">
        <span className="text-xs font-bold text-on-surface">{Math.round(progress)}%</span>
      </div>
    </div>
  )
}

interface VideoPromotionGroup {
  videoId: string
  videoTitle: string
  thumbnailUrl: string | null
  fileName: string
  records: PromotionRecord[]
  createdAt: string
  overallStatus: PromotionStatus
  successRate: number
}

function groupByVideo(records: PromotionRecord[], videos: Video[]): VideoPromotionGroup[] {
  const groups: Map<string, VideoPromotionGroup> = new Map()

  for (const record of records) {
    const video = videos.find(v => v.videoId === record.videoId)
    if (!groups.has(record.videoId)) {
      const videoRecords = records.filter(r => r.videoId === record.videoId)
      const completedCount = videoRecords.filter(r => r.status === 'COMPLETED').length
      const totalForVideo = videoRecords.length
      const statuses = videoRecords.map(r => r.status)
      
      let overallStatus: PromotionStatus = 'COMPLETED'
      if (statuses.some(s => s === 'EXECUTING')) overallStatus = 'EXECUTING'
      else if (statuses.some(s => s === 'PENDING')) overallStatus = 'PENDING'
      else if (statuses.some(s => s === 'FAILED')) overallStatus = 'FAILED'

      const existingGroup = groups.get(record.videoId)
      if (!existingGroup) {
        groups.set(record.videoId, {
          videoId: record.videoId,
          videoTitle: video?.fileName ?? 'Unknown Video',
          thumbnailUrl: video?.thumbnailUrl ?? null,
          fileName: video?.fileName ?? 'Unknown',
          records: [],
          createdAt: record.createdAt,
          overallStatus,
          successRate: totalForVideo > 0 ? (completedCount / totalForVideo) * 100 : 0,
        })
      }
    }
    const group = groups.get(record.videoId)
    if (group) {
      group.records.push(record)
    }
  }

  return Array.from(groups.values()).sort((a, b) => 
    new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  )
}

interface InsightCardProps {
  value: string
  description: string
  icon: string
  bgClass: string
  textClass: string
  labelTextClass: string
  badgeText?: string
}

function InsightCard({ value, description, icon, bgClass, textClass, labelTextClass, badgeText }: InsightCardProps) {
  return (
    <div className={`${bgClass} rounded-xl p-6 flex flex-col justify-between h-[160px]`}>
      <div className="flex justify-between items-start">
        <Icon name={icon} size={20} className={textClass} />
        {badgeText && (
          <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full uppercase ${labelTextClass}`}>
            {badgeText}
          </span>
        )}
      </div>
      <div>
        <div className={`text-2xl font-extrabold font-headline ${textClass}`}>{value}</div>
        <div className={`text-sm font-medium mt-1 ${labelTextClass}`}>{description}</div>
      </div>
    </div>
  )
}

type ExpandedRecord = PromotionRecord | ChannelExecutionSummary

interface ExpandedDetailProps {
  records: ExpandedRecord[]
  reportLoading: boolean
  onRetry: (promotionRecordId: string) => void
}

function ExpandedDetail({ records, reportLoading, onRetry }: ExpandedDetailProps) {
  const getRecordId = (record: ExpandedRecord): string =>
    'promotionRecordId' in record ? record.promotionRecordId : record.channelId

  const getCreatedAt = (record: ExpandedRecord): string =>
    'createdAt' in record ? record.createdAt : record.executedAt ?? ''

  const canRetry = (record: ExpandedRecord): boolean =>
    'promotionRecordId' in record && record.status === 'FAILED'

  if (reportLoading) {
    return (
      <div className="bg-surface-container-low/30 px-8 py-4">
        <div className="flex items-center justify-center gap-3 py-8">
          <div className="w-6 h-6 border-2 border-primary border-t-transparent rounded-full animate-spin" />
          <span className="text-sm text-on-surface-variant">加载详情...</span>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-surface-container-low/30 px-8 py-4">
      <div className="space-y-4">
        {records.map(record => {
          const channelConfig = getChannelConfig(record.channelName)
          return (
            <div key={getRecordId(record)} className="flex items-center gap-4 py-3">
              <div className={`w-8 h-8 rounded-full ${channelConfig.bg} flex items-center justify-center ${channelConfig.text}`}>
                <span className="text-[10px] font-bold">{channelConfig.abbr}</span>
              </div>
              <span className="font-medium text-sm text-on-surface flex-shrink-0">{record.channelName}</span>
              <MethodBadge method={record.method} />
              <StatusBadge status={record.status} className="text-xs" />
              {record.resultUrl ? (
                <a
                  href={record.resultUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-primary text-sm flex items-center gap-1 hover:underline"
                >
                  {record.resultUrl.length > 30 ? record.resultUrl.slice(0, 30) + '...' : record.resultUrl}
                  <Icon name="open_in_new" size={14} />
                </a>
              ) : record.errorMessage ? (
                <span className="text-sm text-error/60 italic">{record.errorMessage}</span>
              ) : (
                <span className="text-sm text-outline">等待生成...</span>
              )}
              {canRetry(record) && 'promotionRecordId' in record && (
                <button
                  type="button"
                  onClick={() => onRetry(record.promotionRecordId)}
                  className="text-primary text-sm flex items-center gap-1 hover:underline ml-auto"
                >
                  <Icon name="refresh" size={14} />
                  重试
                </button>
              )}
              <span className="text-sm text-on-surface-variant ml-auto">
                {record.executedAt ? new Date(record.executedAt).toLocaleString('zh-CN', {
                  month: '2-digit',
                  day: '2-digit',
                  hour: '2-digit',
                  minute: '2-digit',
                }) : formatDate(getCreatedAt(record))}
              </span>
            </div>
          )
        })}
      </div>
    </div>
  )
}

interface PromotionGroupRowProps {
  group: VideoPromotionGroup
  isExpanded: boolean
  onToggleExpand: (videoId: string) => void
  onRetry: (promotionRecordId: string) => void
}

function PromotionGroupRow({ group, isExpanded, onToggleExpand, onRetry }: PromotionGroupRowProps) {
  const { data: reportData, isLoading: reportLoading } = usePromotionReport(
    isExpanded ? group.videoId : ''
  )

  return (
    <>
      <tr
        onClick={() => onToggleExpand(group.videoId)}
        className="cursor-pointer hover:bg-surface-bright transition-colors group"
      >
        <td className="px-6 py-4">
          <Icon
            name={isExpanded ? 'expand_more' : 'chevron_right'}
            size={20}
            className="text-on-surface-variant transition-transform"
          />
        </td>
        <td className="px-6 py-4">
          <div className="flex items-center gap-4">
            <div className="w-16 h-10 rounded overflow-hidden flex-shrink-0 bg-surface-container-high relative">
              {group.thumbnailUrl ? (
                <img
                  src={group.thumbnailUrl}
                  alt={group.videoTitle}
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="w-full h-full bg-surface-container-high flex items-center justify-center">
                  <Icon name="movie" size={20} className="text-on-surface-variant/50" />
                </div>
              )}
              <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                <Icon name="play_arrow" size={20} className="text-white" />
              </div>
            </div>
            <span className="text-sm font-semibold text-on-surface truncate max-w-[180px]">
              {group.videoTitle}
            </span>
          </div>
        </td>
        <td className="px-6 py-4 text-sm text-on-surface-variant">
          {group.records.length} 个渠道
        </td>
        <td className="px-6 py-4">
          <CircularProgress progress={group.successRate} />
        </td>
        <td className="px-6 py-4">
          <StatusBadge status={group.overallStatus} showPulse={group.overallStatus === 'EXECUTING'} />
        </td>
        <td className="px-6 py-4 text-sm text-on-surface-variant font-medium">
          {new Date(group.createdAt).toLocaleString('zh-CN', {
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
          })}
        </td>
        <td className="px-6 py-4 text-right">
          <div className="flex items-center justify-end gap-2 opacity-60 group-hover:opacity-100 transition-opacity">
            <button
              type="button"
              className="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all"
              title="查看详情"
            >
              <Icon name="visibility" size={18} />
            </button>
          </div>
        </td>
      </tr>
      {isExpanded && (
        <tr className="bg-surface-container-lowest">
          <td colSpan={7} className="p-0">
            <ExpandedDetail
              records={reportData?.channelSummaries ?? group.records}
              reportLoading={reportLoading}
              onRetry={onRetry}
            />
          </td>
        </tr>
      )}
    </>
  )
}

export function PromotionHistoryPage() {
  const [selectedVideoId, setSelectedVideoId] = useState<string>('')
  const [startDate, setStartDate] = useState<string>('')
  const [endDate, setEndDate] = useState<string>('')
  const [page, setPage] = useState(1)
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set())
  const pageSize = 20

  const { data: videoListData, isLoading: videosLoading } = useVideoList({ pageSize: 100 })
  const videos = useMemo(() => videoListData?.items ?? [], [videoListData])

  const { data: historyData, isLoading: historyLoading, error: historyError } = usePromotionHistory(
    selectedVideoId || 'all',
    { page, pageSize, startDate, endDate }
  )

  const records = useMemo(() => historyData?.items ?? [], [historyData])
  const pagination = historyData
  const groupedRecords = useMemo(() => groupByVideo(records, videos), [records, videos])

  const retryMutation = useRetryPromotion()

  const handleToggleExpand = useCallback((videoId: string) => {
    setExpandedIds(prev => {
      const newSet = new Set(prev)
      if (newSet.has(videoId)) {
        newSet.delete(videoId)
      } else {
        newSet.add(videoId)
      }
      return newSet
    })
  }, [])

  const handleRetry = useCallback((promotionRecordId: string) => {
    retryMutation.mutate({ promotionRecordId })
  }, [retryMutation])

  const handleExport = useCallback(() => {
    const csvContent = records.map(r => 
      `${r.videoId},${r.channelName},${r.method},${r.status},${r.resultUrl ?? ''},${r.createdAt}`
    ).join('\n')
    const header = 'Video ID,Channel,Method,Status,Result URL,Created At\n'
    const blob = new Blob([header + csvContent], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `promotion-history-${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
  }, [records])

  const handleVideoSelect = useCallback((e: React.ChangeEvent<HTMLSelectElement>) => {
    setSelectedVideoId(e.target.value)
    setPage(1)
    setExpandedIds(new Set())
  }, [])

  const handlePageChange = useCallback((newPage: number) => {
    setPage(newPage)
    setExpandedIds(new Set())
  }, [])

  const overallSuccessRate = useMemo(() => {
    const completed = records.filter(r => r.status === 'COMPLETED').length
    return records.length > 0 ? Math.round((completed / records.length) * 100) : 0
  }, [records])

  const bestMethod = useMemo(() => {
    const methods: Record<PromotionMethod, { success: number; total: number }> = {
      POST: { success: 0, total: 0 },
      COMMENT: { success: 0, total: 0 },
      SHARE: { success: 0, total: 0 },
    }
    for (const r of records) {
      methods[r.method].total++
      if (r.status === 'COMPLETED') methods[r.method].success++
    }
    let best: PromotionMethod = 'POST'
    let bestRate = 0
    for (const [method, stats] of Object.entries(methods)) {
      if (stats.total > 0) {
        const rate = stats.success / stats.total
        if (rate > bestRate) {
          bestRate = rate
          best = method as PromotionMethod
        }
      }
    }
    return best
  }, [records])

  const peakTime = useMemo(() => {
    const hours: Record<number, number> = {}
    for (const r of records) {
      if (r.executedAt) {
        const hour = new Date(r.executedAt).getHours()
        hours[hour] = (hours[hour] || 0) + 1
      }
    }
    let peakHour = 14
    let maxCount = 0
    for (const [hour, count] of Object.entries(hours)) {
      if (count > maxCount) {
        maxCount = count
        peakHour = parseInt(hour)
      }
    }
    return `${peakHour}:00 - ${peakHour + 2}:00`
  }, [records])

  return (
    <div className="p-8 max-w-7xl mx-auto">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-10">
        <div>
          <h1 className="text-[2.75rem] font-extrabold font-headline leading-tight tracking-tight text-on-surface">
            推广历史
          </h1>
          <p className="text-body-md text-slate-500 mt-2">监控和分析您在所有渠道的视频推广效果。</p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative">
            <select
              value={selectedVideoId}
              onChange={handleVideoSelect}
              disabled={videosLoading}
              className="appearance-none bg-surface-container-low border-none rounded-lg px-4 pr-10 py-2.5 text-sm font-medium focus:ring-2 focus:ring-primary/40 transition-all cursor-pointer min-w-[200px]"
            >
              <option value="">所有视频</option>
              {videos.map(video => (
                <option key={video.videoId} value={video.videoId}>
                  {video.fileName}
                </option>
              ))}
            </select>
            <Icon
              name="expand_more"
              size={18}
              className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-on-surface-variant"
            />
          </div>
          <div className="flex items-center bg-surface-container-low rounded-lg px-4 py-2.5 gap-3">
            <Icon name="calendar_today" size={18} className="text-on-surface-variant" />
            <input
              type="date"
              value={startDate}
              onChange={e => setStartDate(e.target.value)}
              className="bg-transparent border-none p-0 text-sm font-medium focus:ring-0"
            />
            <span className="text-on-surface-variant">-</span>
            <input
              type="date"
              value={endDate}
              onChange={e => setEndDate(e.target.value)}
              className="bg-transparent border-none p-0 text-sm font-medium focus:ring-0"
            />
          </div>
          <Button variant="primary" icon="download" onClick={handleExport}>
            导出报告
          </Button>
        </div>
      </div>

      <div className="bg-surface-container-lowest rounded-xl overflow-hidden shadow-[0_8px_32px_rgba(0,26,67,0.06)]">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-surface-container-low/50">
                <th className="px-6 py-4 text-xs font-bold text-on-surface-variant uppercase tracking-wider w-[40px]"></th>
                <th className="px-6 py-4 text-xs font-bold text-on-surface-variant uppercase tracking-wider">视频</th>
                <th className="px-6 py-4 text-xs font-bold text-on-surface-variant uppercase tracking-wider">渠道数</th>
                <th className="px-6 py-4 text-xs font-bold text-on-surface-variant uppercase tracking-wider">进度</th>
                <th className="px-6 py-4 text-xs font-bold text-on-surface-variant uppercase tracking-wider">状态</th>
                <th className="px-6 py-4 text-xs font-bold text-on-surface-variant uppercase tracking-wider">时间</th>
                <th className="px-6 py-4 text-xs font-bold text-on-surface-variant uppercase tracking-wider text-right">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-surface-container-low">
              {historyLoading ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center">
                    <div className="flex flex-col items-center justify-center gap-3">
                      <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                      <span className="text-sm text-on-surface-variant">加载中...</span>
                    </div>
                  </td>
                </tr>
              ) : historyError ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center">
                    <div className="flex flex-col items-center justify-center gap-3">
                      <Icon name="error" size={48} className="text-error" />
                      <p className="text-sm text-on-surface-variant">加载推广历史失败</p>
                      <Button variant="secondary" onClick={() => window.location.reload()}>
                        重试
                      </Button>
                    </div>
                  </td>
                </tr>
              ) : groupedRecords.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center">
                    <div className="flex flex-col items-center justify-center gap-3">
                      <Icon name="task" size={48} className="text-on-surface-variant/30" />
                      <p className="text-sm text-on-surface-variant">暂无推广记录</p>
                    </div>
                  </td>
                </tr>
              ) : (
                groupedRecords.map(group => (
                  <PromotionGroupRow
                    key={group.videoId}
                    group={group}
                    isExpanded={expandedIds.has(group.videoId)}
                    onToggleExpand={handleToggleExpand}
                    onRetry={handleRetry}
                  />
                ))
              )}
            </tbody>
          </table>
        </div>

        {pagination && pagination.totalPages > 1 && (
          <div className="px-6 py-4 bg-surface-container-low/20 flex items-center justify-between">
            <span className="text-xs font-medium text-on-surface-variant">
              显示 {(pagination.page - 1) * pagination.pageSize + 1}-{Math.min(pagination.page * pagination.pageSize, pagination.total)} 条记录 (共 {pagination.total} 条)
            </span>
            <Pagination
              page={pagination.page}
              totalPages={pagination.totalPages}
              onPageChange={handlePageChange}
            />
          </div>
        )}
      </div>

      <div className="grid grid-cols-3 gap-6 mt-10">
        <InsightCard
          value={`${overallSuccessRate}%`}
          description="本月平均发布成功率"
          icon="auto_awesome"
          bgClass="bg-tertiary-fixed"
          textClass="text-on-tertiary-fixed"
          labelTextClass="bg-on-tertiary-fixed/10 text-on-tertiary-fixed"
          badgeText="Insight"
        />
        <InsightCard
          value={bestMethod}
          description="最受欢迎的推广方式"
          icon="trending_up"
          bgClass="bg-secondary-fixed"
          textClass="text-on-secondary-fixed"
          labelTextClass="bg-on-secondary-fixed/10 text-on-secondary-fixed"
          badgeText="Method"
        />
        <InsightCard
          value={peakTime}
          description="用户互动最频繁时段"
          icon="schedule"
          bgClass="bg-surface-container-high"
          textClass="text-on-surface"
          labelTextClass="bg-on-surface/5 text-on-surface-variant"
          badgeText="Peak Time"
        />
      </div>
    </div>
  )
}