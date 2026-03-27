import apiClient from './client'
import type {
  PromotionCopy,
  PromotionRecord,
  PromotionResult,
  PromotionReport,
  ExecutePromotionRequest,
  GenerateCopyRequest,
  PromotionHistoryParams,
  RetryPromotionRequest,
} from '@/types/promotion'
import type { PaginatedData, ApiResponse } from '@/types/common'

// F1: POST /api/promotions/generate-copy
export async function generateCopy(
  request: GenerateCopyRequest
): Promise<PromotionCopy[]> {
  const response = await apiClient.post<ApiResponse<PromotionCopy[]>>(
    '/promotions/generate-copy',
    request
  )
  return response.data.data
}

// F2: POST /api/promotions/execute
export async function executePromotion(
  request: ExecutePromotionRequest
): Promise<PromotionResult[]> {
  const response = await apiClient.post<ApiResponse<PromotionResult[]>>(
    '/promotions/execute',
    request
  )
  return response.data.data
}

// F3: GET /api/promotions/history/{videoId}
export async function getPromotionHistory(
  videoId: string,
  params?: PromotionHistoryParams
): Promise<PaginatedData<PromotionRecord>> {
  const response = await apiClient.get<ApiResponse<PaginatedData<PromotionRecord>>>(
    `/promotions/history/${videoId}`,
    {
      params: {
        page: params?.page ?? 1,
        pageSize: params?.pageSize ?? 20,
        status: params?.status,
        channelId: params?.channelId,
        startDate: params?.startDate,
        endDate: params?.endDate,
      },
    }
  )
  return response.data.data
}

// F4: GET /api/promotions/report/{videoId}
export async function getPromotionReport(videoId: string): Promise<PromotionReport> {
  const response = await apiClient.get<ApiResponse<PromotionReport>>(
    `/promotions/report/${videoId}`
  )
  return response.data.data
}

// F5: POST /api/promotions/{promotionRecordId}/retry
export async function retryPromotion(
  promotionRecordId: string,
  request?: RetryPromotionRequest
): Promise<PromotionResult> {
  const response = await apiClient.post<ApiResponse<PromotionResult>>(
    `/promotions/${promotionRecordId}/retry`,
    request ?? {}
  )
  return response.data.data
}
