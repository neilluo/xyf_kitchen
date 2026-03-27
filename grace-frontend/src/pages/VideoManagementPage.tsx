import { useState, useCallback, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useVideoList } from '@/hooks/useVideos'
import { Button } from '@/components/ui/Button'
import { Icon } from '@/components/ui/Icon'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { Pagination } from '@/components/ui/Pagination'
import type { VideoListParams, VideoStatus } from '@/types/video'
import { formatFileSize, formatDuration } from '@/utils/format'

// 状态选项
const STATUS_OPTIONS: { value: VideoStatus | ''; label: string }[] = [
  { value: '', label: '全部状态' },
  { value: 'UPLOADED', label: '已上传' },
  { value: 'METADATA_GENERATED', label: '元数据已生成' },
  { value: 'READY_TO_PUBLISH', label: '待发布' },
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'PUBLISH_FAILED', label: '发布失败' },
]

export function VideoManagementPage() {
  const navigate = useNavigate()
  const [params, setParams] = useState<VideoListParams>({
    page: 1,
    pageSize: 10,
    keyword: '',
    status: '',
    sort: 'createdAt',
    order: 'desc',
  })
  const [debouncedKeyword, setDebouncedKeyword] = useState('')

  // 防抖搜索 300ms
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedKeyword(params.keyword || '')
    }, 300)
    return () => clearTimeout(timer)
  }, [params.keyword])

  const { data, isLoading, error } = useVideoList({
    ...params,
    keyword: debouncedKeyword,
  })

  const videos = data?.data?.list || []
  const pagination = data?.data?.pagination

  const handleKeywordChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const keyword = e.target.value
    setParams(prev => ({ ...prev, keyword, page: 1 }))
  }, [])

  const handleStatusChange = useCallback((e: React.ChangeEvent<HTMLSelectElement>) => {
    const status = e.target.value as VideoStatus | ''
    setParams(prev => ({ ...prev, status, page: 1 }))
  }, [])

  const handlePageChange = useCallback((page: number) => {
    setParams(prev => ({ ...prev, page }))
  }, [])

  const handleNavigateToUpload = useCallback(() => {
    navigate('/upload')
  }, [navigate])

  const handleViewDetail = useCallback((videoId: string) => {
    navigate(`/videos/${videoId}/metadata`)
  }, [navigate])

  const handleEditMetadata = useCallback((videoId: string) => {
    navigate(`/videos/${videoId}/metadata`)
  }, [navigate])

  const handleDistribute = useCallback((videoId: string) => {
    navigate(`/videos/${videoId}/distribute`)
  }, [navigate])

  return (
    <div className="p-8 max-w-7xl mx-auto">
      {/* Page Header */}
      <div className="flex items-end justify-between mb-10">
        <div>
          <h1 className="text-[2.75rem] font-extrabold font-headline leading-tight tracking-tight text-on-surface">
            视频管理
          </h1>
          <p className="text-body-md text-slate-500 mt-2">管理所有已上传的视频及其分发状态</p>
        </div>
        <div className="flex gap-3">
          <Button
            variant="secondary"
            icon="refresh"
            onClick={() => window.location.reload()}
          >
            刷新
          </Button>
          <Button
            variant="primary"
            icon="add"
            onClick={handleNavigateToUpload}
          >
            上传视频
          </Button>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="bg-surface-container-low p-4 rounded-xl flex flex-wrap items-center gap-4 mb-6">
        <div className="flex-1 min-w-[300px] relative">
          <Icon
            name="search"
            size={20}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/50"
          />
          <input
            type="text"
            value={params.keyword}
            onChange={handleKeywordChange}
            placeholder="搜索视频文件名..."
            className="w-full bg-surface-container-lowest border-none rounded-lg py-2.5 pl-10 pr-4 text-sm focus:ring-2 focus:ring-primary/40 transition-all placeholder:text-on-surface-variant/50"
          />
        </div>
        <div className="flex items-center gap-4">
          <select
            value={params.status}
            onChange={handleStatusChange}
            className="bg-surface-container-lowest border-none rounded-lg py-2.5 px-4 text-sm focus:ring-2 focus:ring-primary/40 transition-all text-on-surface min-w-[140px] cursor-pointer"
          >
            {STATUS_OPTIONS.map(option => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <div className="bg-surface-container-lowest flex items-center rounded-lg overflow-hidden border-none focus-within:ring-2 focus-within:ring-primary/40 transition-all">
            <Icon name="calendar_today" size={18} className="pl-3 text-on-surface-variant/50" />
            <input
              type="text"
              placeholder="选择日期范围"
              className="bg-transparent border-none py-2.5 px-3 text-sm focus:ring-0 placeholder:text-on-surface-variant/50"
              readOnly
            />
          </div>
        </div>
      </div>

      {/* Data Table Card */}
      <div className="bg-surface-container-lowest rounded-xl overflow-hidden shadow-sm">
        <table className="w-full text-left border-collapse">
          <thead className="bg-surface-container-low/50">
            <tr>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">预览</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">文件名</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">时长</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">状态</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">上传时间</th>
              <th className="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider text-right">操作</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant/10">
            {isLoading ? (
              <tr>
                <td colSpan={6} className="px-6 py-12 text-center">
                  <div className="flex flex-col items-center justify-center gap-3">
                    <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                    <span className="text-sm text-on-surface-variant">加载中...</span>
                  </div>
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan={6} className="px-6 py-12 text-center">
                  <div className="flex flex-col items-center justify-center gap-3">
                    <Icon name="error" size={48} className="text-error" />
                    <p className="text-sm text-on-surface-variant">加载视频列表失败</p>
                    <Button variant="secondary" onClick={() => window.location.reload()}>
                      重试
                    </Button>
                  </div>
                </td>
              </tr>
            ) : videos.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-6 py-12 text-center">
                  <div className="flex flex-col items-center justify-center gap-3">
                    <Icon name="video_library" size={48} className="text-on-surface-variant/30" />
                    <p className="text-sm text-on-surface-variant">暂无视频，去上传第一个视频吧</p>
                    <Button variant="primary" onClick={handleNavigateToUpload}>
                      上传视频
                    </Button>
                  </div>
                </td>
              </tr>
            ) : (
              videos.map(video => (
                <tr
                  key={video.videoId}
                  className="hover:bg-surface-bright transition-colors group"
                >
                  {/* Thumbnail */}
                  <td className="px-6 py-4">
                    <div className="w-[80px] h-[45px] rounded bg-slate-200 overflow-hidden relative">
                      {video.thumbnailUrl ? (
                        <img
                          src={video.thumbnailUrl}
                          alt={video.fileName}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <div className="w-full h-full bg-surface-container-high flex items-center justify-center">
                          <Icon name="movie" size={24} className="text-on-surface-variant/50" />
                        </div>
                      )}
                      <div className="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer">
                        <Icon name="play_arrow" size={24} className="text-white" />
                      </div>
                    </div>
                  </td>

                  {/* File Name + Format Badge */}
                  <td className="px-6 py-4">
                    <div>
                      <p className="text-sm font-bold text-on-surface">{video.fileName}</p>
                      <p className="text-[11px] text-slate-400 mt-0.5">
                        {video.format} · {formatFileSize(video.fileSize)}
                      </p>
                    </div>
                  </td>

                  {/* Duration */}
                  <td className="px-6 py-4 text-sm text-on-surface-variant">
                    {formatDuration(video.duration)}
                  </td>

                  {/* Status */}
                  <td className="px-6 py-4">
                    <StatusBadge status={video.status} />
                  </td>

                  {/* Upload Date */}
                  <td className="px-6 py-4 text-sm text-on-surface-variant">
                    {new Date(video.createdAt).toLocaleString('zh-CN', {
                      year: 'numeric',
                      month: '2-digit',
                      day: '2-digit',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </td>

                  {/* Actions */}
                  <td className="px-6 py-4 text-right">
                    <div className="flex items-center justify-end gap-2 opacity-60 group-hover:opacity-100 transition-opacity">
                      <button
                        type="button"
                        className="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all"
                        onClick={() => handleViewDetail(video.videoId)}
                        title="查看详情"
                      >
                        <Icon name="visibility" size={18} />
                      </button>
                      <button
                        type="button"
                        className="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all"
                        onClick={() => handleEditMetadata(video.videoId)}
                        title="编辑元数据"
                      >
                        <Icon name="edit" size={18} />
                      </button>
                      <button
                        type="button"
                        className="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all"
                        onClick={() => handleDistribute(video.videoId)}
                        title="分发"
                      >
                        <Icon name="send" size={18} />
                      </button>
                      <button
                        type="button"
                        className="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all"
                        title="更多"
                      >
                        <Icon name="more_vert" size={18} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Pagination Footer */}
        {pagination && pagination.totalPages > 1 && (
          <div className="px-6 py-4 bg-surface-container-lowest border-t border-outline-variant/10 flex items-center justify-between">
            <div className="flex items-center gap-4 text-xs text-on-surface-variant">
              <span>显示 {(pagination.page - 1) * pagination.pageSize + 1}-{Math.min(pagination.page * pagination.pageSize, pagination.total)} 共 {pagination.total} 条数据</span>
            </div>
            <Pagination
              page={pagination.page}
              totalPages={pagination.totalPages}
              onPageChange={handlePageChange}
            />
          </div>
        )}
      </div>
    </div>
  )
}
