import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AppLayout } from '@/components/layout/AppLayout'
import DashboardPage from '@/pages/DashboardPage'
import { VideoManagementPage } from '@/pages/VideoManagementPage'
import { VideoUploadPage } from '@/pages/VideoUploadPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

function MetadataReviewPage() {
  return (
    <div className="p-4">
      <h1 className="text-2xl font-bold text-on-surface">元数据审核</h1>
      <p className="text-on-surface-variant mt-2">Metadata review page placeholder</p>
    </div>
  )
}

function DistributionPromotionPage() {
  return (
    <div className="p-4">
      <h1 className="text-2xl font-bold text-on-surface">分发与推广</h1>
      <p className="text-on-surface-variant mt-2">Distribution and promotion page placeholder</p>
    </div>
  )
}

function PromotionHistoryPage() {
  return (
    <div className="p-4">
      <h1 className="text-2xl font-bold text-on-surface">推广历史</h1>
      <p className="text-on-surface-variant mt-2">Promotion history page placeholder</p>
    </div>
  )
}

function SettingsPage() {
  return (
    <div className="p-4">
      <h1 className="text-2xl font-bold text-on-surface">设置</h1>
      <p className="text-on-surface-variant mt-2">Settings page placeholder</p>
    </div>
  )
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route element={<AppLayout />}>
            <Route index element={<DashboardPage />} />
            <Route path="videos" element={<VideoManagementPage />} />
            <Route path="upload" element={<VideoUploadPage />} />
            <Route path="videos/:videoId/metadata" element={<MetadataReviewPage />} />
            <Route path="videos/:videoId/distribute" element={<DistributionPromotionPage />} />
            <Route path="promotions" element={<PromotionHistoryPage />} />
            <Route path="settings" element={<SettingsPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

export default App
