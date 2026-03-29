import { NavLink, useLocation } from 'react-router-dom'
import { Icon } from '@/components/ui/Icon'

interface NavItem {
  icon: string
  label: string
  path: string
}

const navItems: NavItem[] = [
  { icon: 'dashboard', label: '仪表盘', path: '/' },
  { icon: 'video_library', label: '视频管理', path: '/videos' },
  { icon: 'upload', label: '上传视频', path: '/upload' },
  { icon: 'history', label: '推广历史', path: '/promotions' },
  { icon: 'settings', label: '设置', path: '/settings' },
]

export function Sidebar() {
  const location = useLocation()

  const isActive = (path: string) => {
    if (path === '/') {
      return location.pathname === '/'
    }
    return location.pathname.startsWith(path)
  }

  return (
    <aside className="fixed left-0 top-0 h-full w-[240px] bg-[#001529] flex flex-col z-50">
      {/* Logo */}
      <div className="py-8 px-6 flex items-center gap-3">
        <Icon name="restaurant_menu" className="text-white" size={28} />
        <span className="text-white font-headline text-xl font-bold">Grace</span>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto">
        {navItems.map((item) => {
          const active = isActive(item.path)
          return (
            <NavLink
              key={item.path}
              to={item.path}
              className={`py-4 px-6 flex items-center gap-3 text-sm font-body transition-colors ${
                active
                  ? 'bg-blue-600/10 text-blue-400 border-r-4 border-blue-500'
                  : 'text-slate-400 hover:text-white hover:bg-white/5'
              }`}
            >
              <Icon name={item.icon} size={20} />
              <span>{item.label}</span>
            </NavLink>
          )
        })}
      </nav>
    </aside>
  )
}
