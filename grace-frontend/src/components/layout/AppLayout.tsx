import { Outlet } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Header } from './Header'

export function AppLayout() {
  return (
    <div className="min-h-screen bg-surface">
      <Sidebar />
      <Header />
      <main className="ml-[240px] pt-24 px-8 pb-8 min-h-screen">
        <Outlet />
      </main>
    </div>
  )
}
