import type { VideoStatus } from '@/types/video'
import type { PromotionStatus } from '@/types/promotion'
import type { PublishStatus } from '@/types/distribution'

interface StatusConfig {
  label: string
  bgClass: string
  textClass: string
}

export const VIDEO_STATUS_MAP: Record<VideoStatus, StatusConfig> = {
  UPLOADED: {
    label: '已上传',
    bgClass: 'bg-surface-container-high',
    textClass: 'text-on-surface-variant',
  },
  METADATA_GENERATED: {
    label: '元数据已生成',
    bgClass: 'bg-surface-container-high',
    textClass: 'text-on-surface-variant',
  },
  READY_TO_PUBLISH: {
    label: '待发布',
    bgClass: 'bg-tertiary-fixed',
    textClass: 'text-on-tertiary-fixed-variant',
  },
  PUBLISHING: {
    label: '发布中',
    bgClass: 'bg-primary-fixed',
    textClass: 'text-on-primary-fixed',
  },
  PUBLISHED: {
    label: '已发布',
    bgClass: 'bg-secondary-fixed',
    textClass: 'text-on-secondary-fixed',
  },
  PUBLISH_FAILED: {
    label: '发布失败',
    bgClass: 'bg-error-container',
    textClass: 'text-on-error-container',
  },
  PROMOTION_DONE: {
    label: '推广完成',
    bgClass: 'bg-secondary-fixed',
    textClass: 'text-on-secondary-fixed',
  },
}

export const PROMOTION_STATUS_MAP: Record<PromotionStatus, StatusConfig> = {
  PENDING: {
    label: '待执行',
    bgClass: 'bg-surface-container-high',
    textClass: 'text-on-surface-variant',
  },
  EXECUTING: {
    label: '进行中',
    bgClass: 'bg-secondary-fixed',
    textClass: 'text-on-secondary-fixed',
  },
  COMPLETED: {
    label: '成功',
    bgClass: 'bg-green-100',
    textClass: 'text-green-800',
  },
  FAILED: {
    label: '失败',
    bgClass: 'bg-error-container',
    textClass: 'text-on-error-container',
  },
}

export const PUBLISH_STATUS_MAP: Record<PublishStatus, StatusConfig> = {
  PENDING: {
    label: '待处理',
    bgClass: 'bg-surface-container-high',
    textClass: 'text-on-surface-variant',
  },
  UPLOADING: {
    label: '上传中',
    bgClass: 'bg-primary-fixed',
    textClass: 'text-on-primary-fixed',
  },
  COMPLETED: {
    label: '发布成功',
    bgClass: 'bg-green-100',
    textClass: 'text-green-800',
  },
  FAILED: {
    label: '发布失败',
    bgClass: 'bg-error-container',
    textClass: 'text-on-error-container',
  },
  QUOTA_EXCEEDED: {
    label: '配额超限',
    bgClass: 'bg-error-container',
    textClass: 'text-on-error-container',
  },
}
