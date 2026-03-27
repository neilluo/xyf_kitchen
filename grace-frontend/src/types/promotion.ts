import type { ChannelType } from './channel'

// 推广方式
export type PromotionMethod = 'POST' | 'COMMENT' | 'SHARE'

// 推广状态 - 4种
export type PromotionStatus = 'PENDING' | 'EXECUTING' | 'COMPLETED' | 'FAILED'

// AI 生成的推广文案
export interface PromotionCopy {
  channelId: string
  channelName: string
  channelType: string
  promotionTitle: string
  promotionBody: string
  recommendedMethod: PromotionMethod
  methodReason: string
}

// 推广记录
export interface PromotionRecord {
  promotionRecordId: string
  videoId: string
  channelId: string
  channelName: string
  channelType: ChannelType
  promotionTitle: string
  promotionBody: string
  method: PromotionMethod
  status: PromotionStatus
  resultUrl: string | null
  errorMessage: string | null
  executedAt: string | null
  createdAt: string
}

// 推广执行请求项
export interface PromotionItem {
  channelId: string
  promotionTitle: string
  promotionBody: string
  method: PromotionMethod
}

// 推广执行请求
export interface ExecutePromotionRequest {
  videoId: string
  promotionItems: PromotionItem[]
}

// 推广结果
export interface PromotionResult {
  promotionRecordId: string
  channelId: string
  channelName: string
  method: PromotionMethod
  status: PromotionStatus
  resultUrl: string | null
  errorMessage: string | null
  executedAt: string
}

// 推广报告
export interface PromotionReport {
  videoId: string
  videoTitle: string
  totalChannels: number
  successCount: number
  failedCount: number
  pendingCount: number
  overallSuccessRate: number
  channelSummaries: ChannelExecutionSummary[]
}

// 渠道执行摘要
export interface ChannelExecutionSummary {
  channelId: string
  channelName: string
  channelType: string
  method: PromotionMethod
  status: PromotionStatus
  resultUrl: string | null
  errorMessage: string | null
  executedAt: string | null
}

// 推广历史查询参数
export interface PromotionHistoryParams {
  page?: number
  pageSize?: number
  status?: PromotionStatus
  channelId?: string
  startDate?: string
  endDate?: string
}

// 重试推广请求
export interface RetryPromotionRequest {
  promotionTitle?: string
  promotionBody?: string
}

// 生成文案请求
export interface GenerateCopyRequest {
  videoId: string
  channelIds?: string[]
}
