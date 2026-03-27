import apiClient from './client'
import type { DashboardOverview, DashboardParams } from '@/types/dashboard'
import type { ApiResponse } from '@/types/common'

// A1: GET /api/dashboard/overview
export async function getDashboardOverview(params?: DashboardParams): Promise<DashboardOverview> {
  const response = await apiClient.get<ApiResponse<DashboardOverview>>('/dashboard/overview', {
    params: {
      dateRange: params?.dateRange ?? '30d',
    },
  })
  return response.data.data
}
