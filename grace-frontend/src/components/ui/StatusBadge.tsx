// StatusBadge - 状态徽章组件
// 根据设计系统 §5.2 实现 10 种状态映射

import { cn } from '@/lib/utils'

export type VideoStatus =
  | 'UPLOADED'
  | 'METADATA_GENERATED'
  | 'METADATA_APPROVED'
  | 'METADATA_REJECTED'
  | 'PUBLISHING'
  | 'PUBLISHED'
  | 'PUBLISH_FAILED'

export type PromotionStatus =
  | 'PENDING'
  | 'EXECUTING'
  | 'COMPLETED'
  | 'FAILED'

export type PublishStatus =
  | 'PENDING'
  | 'PUBLISHING'
  | 'PUBLISHED'
  | 'FAILED'
  | 'READY_TO_PUBLISH'

type StatusType = VideoStatus | PromotionStatus | PublishStatus

interface StatusConfig {
  bgClass: string
  textClass: string
  label: string
}

const statusMap: Record<string, StatusConfig> = {
  // Video 状态
  UPLOADED: {
    bgClass: 'bg-surface-container-high',
    textClass: 'text-on-surface-variant',
    label: '已上传',
  },
  METADATA_GENERATED: {
    bgClass: 'bg-surface-container-high',
    textClass: 'text-on-surface-variant',
    label: '元数据已生成',
  },
  METADATA_APPROVED: {
    bgClass: 'bg-secondary-fixed',
    textClass: 'text-on-secondary-fixed',
    label: '元数据已审核',
  },
  METADATA_REJECTED: {
    bgClass: 'bg-error-container',
    textClass: 'text-on-error-container',
    label: '元数据已拒绝',
  },
  PUBLISHING: {
    bgClass: 'bg-primary-fixed',
    textClass: 'text-on-primary-fixed',
    label: '发布中',
  },
  PUBLISHED: {
    bgClass: 'bg-secondary-fixed',
    textClass: 'text-on-secondary-fixed',
    label: '已发布',
  },
  PUBLISH_FAILED: {
    bgClass: 'bg-error-container',
    textClass: 'text-on-error-container',
    label: '发布失败',
  },
  // Promotion 状态
  PENDING: {
    bgClass: 'bg-tertiary-fixed',
    textClass: 'text-on-tertiary-fixed',
    label: '待处理',
  },
  EXECUTING: {
    bgClass: 'bg-secondary-fixed',
    textClass: 'text-on-secondary-fixed',
    label: '进行中',
  },
  // 通用状态
  COMPLETED: {
    bgClass: 'bg-green-100',
    textClass: 'text-green-800',
    label: '成功',
  },
  FAILED: {
    bgClass: 'bg-error-container',
    textClass: 'text-on-error-container',
    label: '失败',
  },
  READY_TO_PUBLISH: {
    bgClass: 'bg-tertiary-fixed',
    textClass: 'text-on-tertiary-fixed',
    label: '待发布',
  },
}

interface StatusBadgeProps {
  status: StatusType
  className?: string
  showPulse?: boolean
}

export function StatusBadge({ status, className, showPulse }: StatusBadgeProps) {
  const config = statusMap[status] || {
    bgClass: 'bg-surface-container-high',
    textClass: 'text-on-surface-variant',
    label: status,
  }

  const isExecuting = status === 'EXECUTING' || status === 'PUBLISHING'

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium',
        config.bgClass,
        config.textClass,
        className
      )}
    >
      {(isExecuting || showPulse) && (
        <span className="relative flex h-1.5 w-1.5">
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-current opacity-75" />
          <span className="relative inline-flex rounded-full h-1.5 w-1.5 bg-current" />
        </span>
      )}
      {config.label}
    </span>
  )
}
