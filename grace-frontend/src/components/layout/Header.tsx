import { Icon } from '@/components/ui/Icon'
import { useLocation } from 'react-router-dom'

const routeTitles: Record<string, string> = {
  '/': '仪表盘',
  '/videos': '视频管理',
  '/upload': '上传视频',
  '/promotions': '推广渠道',
  '/settings': '设置',
}

export function Header() {
  const location = useLocation()

  const getPageTitle = () => {
    const path = location.pathname
    // Check exact matches first
    if (routeTitles[path]) {
      return routeTitles[path]
    }
    // Check dynamic routes
    if (path.startsWith('/videos/') && path.includes('/metadata')) {
      return '元数据审核'
    }
    if (path.startsWith('/videos/') && path.includes('/distribute')) {
      return '视频发布'
    }
    if (path.startsWith('/promotions/')) {
      return '推广任务'
    }
    return 'Grace'
  }

  return (
    <header className="fixed top-0 left-[240px] right-0 h-16 bg-white/80 backdrop-blur-md z-40 flex items-center justify-between px-8">
      {/* Page Title */}
      <h1 className="font-headline text-xl font-bold text-on-surface">
        {getPageTitle()}
      </h1>

      {/* Right Actions */}
      <div className="flex items-center gap-4">
        {/* Notification Bell */}
        <button className="p-2 rounded-full hover:bg-surface-container-low transition-colors relative">
          <Icon name="notifications" className="text-on-surface-variant" size={20} />
          {/* Notification Badge */}
          <span className="absolute top-1 right-1 w-2 h-2 bg-error rounded-full" />
        </button>

        {/* User Avatar */}
        <button className="flex items-center gap-2 p-2 rounded-lg hover:bg-surface-container-low transition-colors">
          <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center">
            <span className="text-white text-sm font-medium">U</span>
          </div>
          <Icon name="expand_more" className="text-on-surface-variant" size={16} />
        </button>
      </div>
    </header>
  )
}
