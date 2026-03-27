import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  generateCopy,
  executePromotion,
  getPromotionHistory,
  getPromotionReport,
  retryPromotion,
} from '@/api/promotion'
import type {
  GenerateCopyRequest,
  ExecutePromotionRequest,
  PromotionHistoryParams,
  RetryPromotionRequest,
} from '@/types/promotion'

// Query key constants
export const promotionKeys = {
  all: ['promotions'] as const,
  copy: (videoId: string) => [...promotionKeys.all, 'copy', videoId] as const,
  history: (videoId: string, params?: PromotionHistoryParams) =>
    [...promotionKeys.all, 'history', videoId, params] as const,
  report: (videoId: string) => [...promotionKeys.all, 'report', videoId] as const,
}

// Generate promotion copy mutation
export function useGenerateCopy() {
  return useMutation({
    mutationFn: (request: GenerateCopyRequest) => generateCopy(request),
  })
}

// Execute promotion mutation
export function useExecutePromotion() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: ExecutePromotionRequest) => executePromotion(request),
    onSuccess: (_, request) => {
      // Invalidate promotion history for the video
      queryClient.invalidateQueries({
        queryKey: promotionKeys.history(request.videoId),
      })
      // Invalidate video lists to reflect promotion status change
      queryClient.invalidateQueries({ queryKey: ['videos'] })
    },
  })
}

// Promotion history query
export function usePromotionHistory(videoId: string, params?: PromotionHistoryParams) {
  return useQuery({
    queryKey: promotionKeys.history(videoId, params),
    queryFn: () => getPromotionHistory(videoId, params),
    enabled: !!videoId,
  })
}

// Promotion report query
export function usePromotionReport(videoId: string) {
  return useQuery({
    queryKey: promotionKeys.report(videoId),
    queryFn: () => getPromotionReport(videoId),
    enabled: !!videoId,
  })
}

// Retry promotion mutation
export function useRetryPromotion() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      promotionRecordId,
      request,
    }: {
      promotionRecordId: string
      request?: RetryPromotionRequest
    }) => retryPromotion(promotionRecordId, request),
    onSuccess: () => {
      // Invalidate all promotion histories since we don't know the videoId
      queryClient.invalidateQueries({ queryKey: promotionKeys.all })
    },
  })
}
