import { useQuery } from '@tanstack/react-query'
import { getDashboardOverview } from '@/api/dashboard'
import type { DateRange } from '@/types/dashboard'

// Query key constants
export const dashboardKeys = {
  all: ['dashboard'] as const,
  overview: (dateRange: DateRange) => [...dashboardKeys.all, 'overview', dateRange] as const,
}

// Dashboard overview query
export function useDashboardOverview(dateRange: DateRange = '30d') {
  return useQuery({
    queryKey: dashboardKeys.overview(dateRange),
    queryFn: () => getDashboardOverview({ dateRange }),
  })
}
