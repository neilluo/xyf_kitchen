# 页面：仪表盘 (Dashboard)

> 依赖文档：[02-design-system.md](./02-design-system.md)、[03-shared-infrastructure.md](./03-shared-infrastructure.md)
> 设计稿：`ui/stitch_grace_video_management/grace_dashboard/screen.png`
> HTML 原型：`ui/stitch_grace_video_management/grace_dashboard/code.html`
> 路由：`/`

## A. 页面概览

仪表盘是用户登录后的首页，聚合展示视频分发与推广的整体数据概览。包含统计卡片、最近上传列表、发布状态分布图和推广渠道成功率。

## B. 组件层级树

```
DashboardPage
├── PageHeader ("仪表盘" 标题 + 日期范围选择器)
├── StatsCardGrid (4 列)
│   ├── StatsCard (总视频数, primary border)
│   ├── StatsCard (待审核, orange border)
│   ├── StatsCard (已发布, green border)
│   └── StatsCard (推广中, tertiary border)
├── ContentGrid (2 列)
│   ├── RecentUploadsCard
│   │   └── RecentUploadsTable
│   │       └── RecentUploadRow × N
│   │           ├── Thumbnail (w-20 h-12)
│   │           ├── FileName
│   │           ├── Date
│   │           └── StatusBadge
│   └── PublishDistributionCard
│       └── DonutChart (SVG)
│           ├── Chart Ring
│           └── Center Label (总数)
└── PromotionOverviewCard
    └── ChannelRow × N
        ├── ChannelName
        ├── ProgressBar
        └── PercentLabel
```

## C. API 端点

| 端点 | 方法 | Hook | 调用时机 |
|------|------|------|----------|
| A1. `/api/dashboard/overview` | GET | `useDashboardOverview(dateRange)` | 页面加载、日期范围变更 |

### 请求参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| dateRange | string | `30d` | `7d` / `30d` / `90d` / `all` |

### 响应数据结构

```typescript
interface DashboardOverview {
  stats: {
    totalVideos: number
    pendingReview: number
    published: number
    promoting: number
  }
  recentUploads: RecentUpload[]
  publishDistribution: {
    published: number
    pending: number
    failed: number
  }
  promotionOverview: PromotionOverviewItem[]
  analytics: {
    avgEngagementRate: number
    totalImpressions: number
  }
}

interface RecentUpload {
  videoId: string
  fileName: string
  thumbnailUrl: string | null
  status: VideoStatus
  createdAt: string
}

interface PromotionOverviewItem {
  channelId: string
  channelName: string
  totalExecutions: number
  successCount: number
  failedCount: number
  successRate: number  // 0.0 - 1.0
}
```

## D. 状态管理

| 类型 | 内容 | 管理方式 |
|------|------|----------|
| 服务端状态 | Dashboard 概览数据 | React Query `useDashboardOverview` |
| 本地状态 | 日期范围选择 `dateRange` | `useState<string>('30d')` |

## E. 关键交互

| 用户操作 | 触发行为 | 状态变更 | UI 反馈 |
|---------|---------|---------|---------|
| 切换日期范围 | 更新 `dateRange` 参数 | React Query 自动 refetch | 数据区域刷新 |
| 点击最近上传的视频行 | 导航到元数据审核页 | `navigate(/videos/${videoId}/metadata)` | 页面跳转 |
| 页面加载 | 查询 A1 | 加载状态 → 数据就绪 | Loading skeleton → 数据渲染 |

## F. 错误处理

| 场景 | 用户提示 | 恢复操作 |
|------|---------|---------|
| A1 请求失败 | "加载仪表盘数据失败" | 显示"重试"按钮 |
| 网络超时 | "网络连接超时" | 自动重试 1 次 |

## G. 视觉实现备注

### StatsCard (统计卡片)

```
容器：bg-surface-container-lowest rounded-lg p-6
左边框：border-l-4
  - 总视频数：border-primary (#0057c2)
  - 待审核：border-orange-400
  - 已发布：border-green-500
  - 推广中：border-tertiary (#7431d3)
数值：font-headline text-3xl font-bold text-on-surface
标签：font-body text-sm text-on-surface-variant mt-1
布局：grid grid-cols-4 gap-6
```

### RecentUploadsTable

```
表头：bg-surface-container-low/50 text-xs uppercase tracking-wider text-on-surface-variant
行：hover:bg-surface-bright transition-colors
缩略图：w-20 h-12 rounded object-cover bg-surface-container-high
文件名：font-body text-sm font-medium text-on-surface
日期：font-body text-xs text-on-surface-variant
状态：StatusBadge 组件
```

### DonutChart (发布状态分布图)

使用 SVG 实现环形图，或使用 Recharts 的 `PieChart`：

```
画布尺寸：约 200×200
环形：stroke-dasharray 计算各段占比
颜色：
  - 已发布 (published)：#0057c2 (primary)
  - 处理中 (pending)：#d5baff (tertiary-fixed-dim)
  - 失败 (failed)：#ba1a1a (error)
中心：total 总数 + "总计" 标签
图例：右侧，各状态圆点 + 标签 + 数量
```

### PromotionOverview (推广概览)

```
渠道行：flex items-center gap-4 py-3
渠道名：font-body text-sm font-medium w-32
进度条：flex-1, h-2 bg-surface-container-highest rounded-full
         内填充 bg-primary rounded-full，宽度 = successRate * 100%
百分比：font-body text-sm font-medium text-on-surface w-12 text-right
```

### 页面整体布局

```
DashboardPage:
  padding: px-8 py-6 (在 AppLayout 的 ml-[240px] pt-24 内)

  统计卡片区: grid grid-cols-4 gap-6
  内容区: grid grid-cols-2 gap-6 mt-6
    左: RecentUploadsCard
    右: PublishDistributionCard
  推广概览: mt-6, full width card
```


## H. HTML 原型参考代码

> 此 HTML 原型使用 CDN Tailwind CSS 构建，包含页面的完整 DOM 结构和精确的 Tailwind class。开发时请参考其中的布局结构、颜色 token、间距和组件样式。

> 源文件：`ui/stitch_grace_video_management/grace_dashboard/code.html`

```html
<!DOCTYPE html>

<html class="light" lang="zh-CN"><head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<title>Grace Platform - Dashboard</title>
<script src="https://cdn.tailwindcss.com?plugins=forms,container-queries"></script>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&amp;family=Manrope:wght@700;800&amp;display=swap" rel="stylesheet"/>
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
<script id="tailwind-config">
      tailwind.config = {
        darkMode: "class",
        theme: {
          extend: {
            colors: {
              "on-secondary": "#ffffff",
              "outline-variant": "#c1c6d7",
              "surface-container-highest": "#e2e2e2",
              "tertiary-container": "#8e4fee",
              "background": "#f9f9f9",
              "on-primary": "#ffffff",
              "secondary": "#4d6077",
              "primary-fixed": "#d9e2ff",
              "on-primary-fixed": "#001a43",
              "primary-container": "#006ef2",
              "surface-variant": "#e2e2e2",
              "error": "#ba1a1a",
              "surface-container-lowest": "#ffffff",
              "on-secondary-fixed-variant": "#35485e",
              "on-error": "#ffffff",
              "surface-container-high": "#e8e8e8",
              "error-container": "#ffdad6",
              "on-surface": "#1a1c1c",
              "on-tertiary": "#ffffff",
              "on-error-container": "#93000a",
              "on-tertiary-fixed-variant": "#5e08bd",
              "inverse-primary": "#afc6ff",
              "secondary-container": "#cde1fd",
              "surface": "#f9f9f9",
              "primary": "#0057c2",
              "tertiary-fixed-dim": "#d5baff",
              "outline": "#727786",
              "primary-fixed-dim": "#afc6ff",
              "surface-bright": "#f9f9f9",
              "on-secondary-fixed": "#071d31",
              "on-tertiary-container": "#fffbff",
              "secondary-fixed-dim": "#b4c8e3",
              "on-primary-container": "#fefcff",
              "tertiary-fixed": "#ecdcff",
              "inverse-on-surface": "#f1f1f1",
              "secondary-fixed": "#d1e4ff",
              "on-background": "#1a1c1c",
              "surface-container-low": "#f3f3f3",
              "tertiary": "#7431d3",
              "on-secondary-container": "#51647c",
              "on-tertiary-fixed": "#270057",
              "surface-dim": "#dadada",
              "inverse-surface": "#2f3131",
              "on-primary-fixed-variant": "#004398",
              "surface-tint": "#0059c7",
              "on-surface-variant": "#414755",
              "surface-container": "#eeeeee"
            },
            fontFamily: {
              "headline": ["Manrope"],
              "body": ["Inter"],
              "label": ["Inter"]
            },
            borderRadius: {"DEFAULT": "0.25rem", "lg": "0.5rem", "xl": "0.75rem", "full": "9999px"},
          },
        },
      }
    </script>
<style>
        .material-symbols-outlined {
            font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
        }
        body { font-family: 'Inter', sans-serif; background-color: #f9f9f9; }
        h1, h2, h3 { font-family: 'Manrope', sans-serif; }
    </style>
</head>
<body class="bg-surface text-on-surface">
<!-- SideNavBar (Shared Component) -->
<aside class="h-screen w-[240px] fixed left-0 top-0 bg-[#001529] flex flex-col justify-between pb-8 z-20">
<div>
<div class="py-8 px-6 text-white text-2xl font-bold flex items-center gap-2 font-headline">
<span class="material-symbols-outlined text-blue-400" data-icon="restaurant_menu">restaurant_menu</span>
                Grace
            </div>
<nav class="mt-4">
<!-- Active Tab: Dashboard -->
<a class="bg-blue-600/10 text-blue-400 border-r-4 border-blue-500 py-4 px-6 flex items-center gap-3 transition-all font-body text-sm" href="#">
<span class="material-symbols-outlined" data-icon="dashboard">dashboard</span>
                    仪表盘
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 flex items-center gap-3 transition-colors hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined" data-icon="video_library">video_library</span>
                    视频管理
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 flex items-center gap-3 transition-colors hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined" data-icon="fact_check">fact_check</span>
                    元数据审核
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 flex items-center gap-3 transition-colors hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined" data-icon="publish">publish</span>
                    视频发布
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 flex items-center gap-3 transition-colors hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined" data-icon="alt_route">alt_route</span>
                    推广渠道
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 flex items-center gap-3 transition-colors hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined" data-icon="task">task</span>
                    推广任务
                </a>
</nav>
</div>
<div class="px-6">
<a class="text-slate-400 hover:text-white py-4 flex items-center gap-3 transition-colors hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined" data-icon="settings">settings</span>
                设置
            </a>
</div>
</aside>
<!-- TopNavBar (Shared Component) -->
<header class="h-16 fixed top-0 right-0 left-[240px] w-[calc(100%-240px)] z-10 bg-white/80 backdrop-blur-md flex items-center justify-between px-8">
<div class="flex items-center gap-2 font-inter text-sm font-medium">
<span class="text-slate-400">主页</span>
<span class="text-slate-300">/</span>
<span class="text-blue-600 font-semibold">仪表盘</span>
</div>
<div class="flex items-center gap-6">
<button class="text-slate-500 hover:bg-slate-50 p-2 rounded-lg transition-all duration-200 relative">
<span class="material-symbols-outlined" data-icon="notifications">notifications</span>
<span class="absolute top-2 right-2 w-2 h-2 bg-error rounded-full border-2 border-white"></span>
</button>
<div class="flex items-center gap-3 cursor-pointer hover:bg-slate-50 p-1 pr-3 rounded-lg transition-all">
<img alt="User Avatar" class="w-8 h-8 rounded-full object-cover" data-alt="Close-up portrait of a professional man with a friendly expression in a brightly lit office environment" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBLZDP-fSs2RBbC959OgYltepawshtxwUrw_bVwaSWbwwBrGnCfqQKn_RS-pbowjFkUviPd0tn-FsD8QjsAuFVKGgDShcIuaOBzkx7f4X5NIW7OUAi2Yq1yGxMrkBQnfl7oa6jIbkxOrxZFZNelPPeqw-MtQqQJXjUc5wYgmXZHfUJSULtqKJeHTGGFuY70cxER-zJLcpBeMRfe4wy49TErv2Clz3Vc_E2QFNcE2LZ4lEzmJRYkta4cAhHu7MgnzhLNkkI-iB2asjHW"/>
<span class="text-sm font-medium text-on-surface">Grace Curator</span>
</div>
</div>
</header>
<!-- Main Content Canvas -->
<main class="ml-[240px] pt-24 px-8 pb-12 min-h-screen">
<!-- Header Section -->
<div class="mb-10">
<h1 class="text-4xl font-extrabold tracking-tight text-on-surface font-headline mb-2">数据概览</h1>
<p class="text-on-surface-variant font-body">欢迎回来，您的视频分发任务运行正常。</p>
</div>
<!-- Top Row: Stat Cards -->
<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
<!-- Total Videos -->
<div class="bg-surface-container-lowest p-6 rounded-lg shadow-[0_8px_32px_rgba(0,26,67,0.04)] flex flex-col justify-between h-32 border-l-4 border-primary">
<div class="flex justify-between items-start">
<span class="text-on-surface-variant font-medium text-sm">总视频数</span>
<span class="material-symbols-outlined text-primary" data-icon="video_library">video_library</span>
</div>
<div class="text-3xl font-extrabold font-headline">24</div>
</div>
<!-- Pending -->
<div class="bg-surface-container-lowest p-6 rounded-lg shadow-[0_8px_32px_rgba(0,26,67,0.04)] flex flex-col justify-between h-32 border-l-4 border-orange-500">
<div class="flex justify-between items-start">
<span class="text-on-surface-variant font-medium text-sm">等待审核</span>
<span class="material-symbols-outlined text-orange-500" data-icon="schedule">schedule</span>
</div>
<div class="text-3xl font-extrabold font-headline">5</div>
</div>
<!-- Published -->
<div class="bg-surface-container-lowest p-6 rounded-lg shadow-[0_8px_32px_rgba(0,26,67,0.04)] flex flex-col justify-between h-32 border-l-4 border-green-500">
<div class="flex justify-between items-start">
<span class="text-on-surface-variant font-medium text-sm">已发布</span>
<span class="material-symbols-outlined text-green-500" data-icon="check_circle">check_circle</span>
</div>
<div class="text-3xl font-extrabold font-headline">16</div>
</div>
<!-- Promoting -->
<div class="bg-surface-container-lowest p-6 rounded-lg shadow-[0_8px_32px_rgba(0,26,67,0.04)] flex flex-col justify-between h-32 border-l-4 border-tertiary">
<div class="flex justify-between items-start">
<span class="text-on-surface-variant font-medium text-sm">推广中</span>
<span class="material-symbols-outlined text-tertiary" data-icon="rocket_launch">rocket_launch</span>
</div>
<div class="text-3xl font-extrabold font-headline">3</div>
</div>
</div>
<!-- Middle Row: Recent Uploads & Publish Stats -->
<div class="grid grid-cols-1 lg:grid-cols-5 gap-8 mb-10">
<!-- Left (60%): Recent Uploads Table -->
<div class="lg:col-span-3 bg-surface-container-lowest p-8 rounded-lg shadow-[0_8px_32px_rgba(0,26,67,0.04)]">
<div class="flex justify-between items-center mb-8">
<h2 class="text-xl font-bold font-headline">最近上传</h2>
<button class="text-primary text-sm font-semibold hover:underline">查看全部</button>
</div>
<div class="overflow-x-auto">
<table class="w-full text-left border-collapse">
<thead>
<tr class="text-on-surface-variant border-b border-surface-container-high">
<th class="pb-4 font-semibold text-sm">视频预览</th>
<th class="pb-4 font-semibold text-sm">文件名</th>
<th class="pb-4 font-semibold text-sm">上传日期</th>
<th class="pb-4 font-semibold text-sm">状态</th>
</tr>
</thead>
<tbody class="divide-y divide-surface-container-low">
<tr class="group hover:bg-surface-bright transition-colors">
<td class="py-4">
<div class="w-20 h-12 rounded bg-surface-container-low overflow-hidden">
<img class="w-full h-full object-cover" data-alt="Vibrant gourmet dish featuring slow-cooked ribs with fresh herbs and a glossy balsamic reduction on a white plate" src="https://lh3.googleusercontent.com/aida-public/AB6AXuB1tloIp2ceIf94DX6liZT6Tb3gy3HvVxHrX8YOgUHgFRif7FHdS1sfgzY2DG7-zwhxBgogWI9zCjFMBp5_O6YM2kXkLR5ePiJ-WlsxHL0ahUwZWnyBkSRPZQpZL7shLSK7Zu3VbY8SrpIteYx1EvRxfSvcJJgjoKyJKpl7FWMJOjzQ_ja3FSU3VoR775jF1uM10nPbrAnPb_yRqYVlyOLLwdJmlZ90_IbXWu7BmFe0LMSumkR7Xib4C2mIvg7_uHMo2k2vH3NS10b-"/>
</div>
</td>
<td class="py-4 font-medium text-sm">法式红酒烩牛肉.mp4</td>
<td class="py-4 text-on-surface-variant text-sm">2023-10-24</td>
<td class="py-4">
<span class="px-3 py-1 bg-secondary-fixed text-on-secondary-fixed-variant rounded-full text-xs font-medium">已发布</span>
</td>
</tr>
<tr class="group hover:bg-surface-bright transition-colors">
<td class="py-4">
<div class="w-20 h-12 rounded bg-surface-container-low overflow-hidden">
<img class="w-full h-full object-cover" data-alt="Artistic high-angle shot of a classic breakfast spread with pancakes, maple syrup, and fresh berries in warm natural light" src="https://lh3.googleusercontent.com/aida-public/AB6AXuAztcQz6ASYekPngtvI31-7KMIWtbSrMZXxYZ5sEhvguKl7OuNzeOAYEs1hhXoq0NVBFrgQIi25YIi08MfxStztgT2iaDMN2pUvGxSCziLxnsFA9xSiEEWzmI4DICodaH3r6nrVLJ9ViWYOUlCEAQr85LiS6AHbb_u1xG70-mdDIb36AwLH73Xz9MdtaXnyfPr6B5rsizXbUL_NnFrnwanJk6dcu0EIdyAp-6qGO5B6kCbIZ3T_YC5ETQZAz4zlkLQa90MzCD8XHn9d"/>
</div>
</td>
<td class="py-4 font-medium text-sm">周末早午餐系列_01.mp4</td>
<td class="py-4 text-on-surface-variant text-sm">2023-10-23</td>
<td class="py-4">
<span class="px-3 py-1 bg-orange-100 text-orange-700 rounded-full text-xs font-medium">审核中</span>
</td>
</tr>
<tr class="group hover:bg-surface-bright transition-colors">
<td class="py-4">
<div class="w-20 h-12 rounded bg-surface-container-low overflow-hidden">
<img class="w-full h-full object-cover" data-alt="Overhead view of a colorful mediterranean salad with feta, olives, and vibrant vegetables in a rustic ceramic bowl" src="https://lh3.googleusercontent.com/aida-public/AB6AXuCA6jVu7kX3rZ544I4aYQAHDMu7dVMOmGnr9eywW5fDhHUxhDC90bZlAN52iz-yoW4H8cO4ziz6q1Xf_MuhpLONcGBgEOWO-q4Jhf-QHX0nMzOWLGjkW3reojIWxJKTzpE6cNfxwNl4ITlE2GuBxEkW-ckkl_blHBhJmdmM-MJDiNPbcYTM8Q-xFekKuEBlxWh1p9H0qSw8f5Aszd7diLM-AxyPedcc5olQi7KXmm7T8wN3yuDlkPGNTfGfUXIPZMc-YaZ7gMkqfgwG"/>
</div>
</td>
<td class="py-4 font-medium text-sm">希腊沙拉制作教程.mov</td>
<td class="py-4 text-on-surface-variant text-sm">2023-10-22</td>
<td class="py-4">
<span class="px-3 py-1 bg-tertiary-fixed text-on-tertiary-fixed-variant rounded-full text-xs font-medium">推广中</span>
</td>
</tr>
<tr class="group hover:bg-surface-bright transition-colors">
<td class="py-4">
<div class="w-20 h-12 rounded bg-surface-container-low overflow-hidden">
<img class="w-full h-full object-cover" data-alt="Cinematic shot of a chef's hand sprinkling sea salt over a fresh salad in a professional kitchen with bokeh lighting" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBqz4xj-AAvakOm9iRna77CGCDZ31Kw1mfitOKuc7EhcFRssa8X-ahp6aT-DA6tld0Izj7mUpKpkHItqR8K4SOyaeVVyhJEwcHW0U_0msQpLXUBm7bIionxC5BchFV_NiuD9mIQq4zkQI-0lj5BoOETQMsGkeG6WypDhCnwlQYMIJKdYfeglyP6xpC5jF3JVTc-BgaLhG4oucXdWdbDCNXqH7qWXYNsuVzUrMu-dGOca9xrccdy-5wdzoT8mpOGqo8sai7eA2NbZjsu"/>
</div>
</td>
<td class="py-4 font-medium text-sm">厨艺技巧：刀法进阶.mp4</td>
<td class="py-4 text-on-surface-variant text-sm">2023-10-21</td>
<td class="py-4">
<span class="px-3 py-1 bg-error-container text-on-error-container rounded-full text-xs font-medium">发布失败</span>
</td>
</tr>
</tbody>
</table>
</div>
</div>
<!-- Right (40%): Publish Stats Donut Chart -->
<div class="lg:col-span-2 bg-surface-container-lowest p-8 rounded-lg shadow-[0_8px_32px_rgba(0,26,67,0.04)] flex flex-col">
<h2 class="text-xl font-bold font-headline mb-8">发布统计</h2>
<div class="relative flex-grow flex items-center justify-center">
<!-- SVG Donut Simulation -->
<svg class="w-48 h-48" viewbox="0 0 36 36">
<path class="text-surface-container-high" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="currentColor" stroke-dasharray="100, 100" stroke-width="3"></path>
<!-- Published (Green) -->
<path class="text-green-500" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="currentColor" stroke-dasharray="64, 100" stroke-linecap="round" stroke-width="3"></path>
<!-- Pending (Orange) -->
<path class="text-orange-400" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="currentColor" stroke-dasharray="20, 100" stroke-dashoffset="-64" stroke-linecap="round" stroke-width="3"></path>
<!-- Failed (Red) -->
<path class="text-error" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" fill="none" stroke="currentColor" stroke-dasharray="16, 100" stroke-dashoffset="-84" stroke-linecap="round" stroke-width="3"></path>
</svg>
<div class="absolute inset-0 flex flex-col items-center justify-center">
<span class="text-3xl font-extrabold font-headline">25</span>
<span class="text-xs text-on-surface-variant font-medium">总任务</span>
</div>
</div>
<div class="mt-8 grid grid-cols-3 gap-2">
<div class="flex flex-col items-center">
<div class="flex items-center gap-1.5 mb-1">
<span class="w-2 h-2 rounded-full bg-green-500"></span>
<span class="text-[10px] text-on-surface-variant uppercase font-bold tracking-wider">已发布</span>
</div>
<span class="text-lg font-bold">16</span>
</div>
<div class="flex flex-col items-center">
<div class="flex items-center gap-1.5 mb-1">
<span class="w-2 h-2 rounded-full bg-orange-400"></span>
<span class="text-[10px] text-on-surface-variant uppercase font-bold tracking-wider">处理中</span>
</div>
<span class="text-lg font-bold">5</span>
</div>
<div class="flex flex-col items-center">
<div class="flex items-center gap-1.5 mb-1">
<span class="w-2 h-2 rounded-full bg-error"></span>
<span class="text-[10px] text-on-surface-variant uppercase font-bold tracking-wider">失败</span>
</div>
<span class="text-lg font-bold">4</span>
</div>
</div>
</div>
</div>
<!-- Bottom Row: Promotion Overview -->
<div class="bg-surface-container-lowest p-8 rounded-lg shadow-[0_8px_32px_rgba(0,26,67,0.04)]">
<div class="flex justify-between items-center mb-10">
<div>
<h2 class="text-xl font-bold font-headline">推广执行概览</h2>
<p class="text-sm text-on-surface-variant mt-1">分渠道推广成功率及流量表现</p>
</div>
<div class="flex gap-2">
<button class="px-4 py-2 bg-surface-container-low text-xs font-semibold rounded hover:bg-surface-container-high transition-colors">近7天</button>
<button class="px-4 py-2 bg-primary text-white text-xs font-semibold rounded shadow-sm">近30天</button>
</div>
</div>
<div class="space-y-8">
<!-- Weibo -->
<div>
<div class="flex justify-between items-center mb-2">
<div class="flex items-center gap-3">
<div class="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center">
<span class="material-symbols-outlined text-red-600 text-sm" data-icon="share">share</span>
</div>
<span class="font-bold text-sm">Weibo / 微博</span>
</div>
<span class="text-sm font-bold text-primary">85%</span>
</div>
<div class="h-3 w-full bg-surface-container-low rounded-full overflow-hidden">
<div class="h-full bg-primary rounded-full" style="width: 85%"></div>
</div>
</div>
<!-- Reddit -->
<div>
<div class="flex justify-between items-center mb-2">
<div class="flex items-center gap-3">
<div class="w-8 h-8 rounded-full bg-orange-100 flex items-center justify-center">
<span class="material-symbols-outlined text-orange-600 text-sm" data-icon="forum">forum</span>
</div>
<span class="font-bold text-sm">Reddit</span>
</div>
<span class="text-sm font-bold text-primary">72%</span>
</div>
<div class="h-3 w-full bg-surface-container-low rounded-full overflow-hidden">
<div class="h-full bg-primary rounded-full" style="width: 72%"></div>
</div>
</div>
<!-- Douban -->
<div>
<div class="flex justify-between items-center mb-2">
<div class="flex items-center gap-3">
<div class="w-8 h-8 rounded-full bg-green-100 flex items-center justify-center">
<span class="material-symbols-outlined text-green-600 text-sm" data-icon="reviews">reviews</span>
</div>
<span class="font-bold text-sm">Douban / 豆瓣</span>
</div>
<span class="text-sm font-bold text-primary">90%</span>
</div>
<div class="h-3 w-full bg-surface-container-low rounded-full overflow-hidden">
<div class="h-full bg-primary rounded-full" style="width: 90%"></div>
</div>
</div>
</div>
<!-- Dashboard Analytics Inset -->
<div class="mt-12 p-6 bg-surface-container-low rounded-lg flex items-center justify-between">
<div class="flex items-center gap-6">
<div>
<div class="text-[10px] text-on-surface-variant font-bold tracking-wider mb-1">平均互动率</div>
<div class="text-2xl font-extrabold font-headline">4.2%</div>
</div>
<div class="h-10 w-px bg-outline-variant opacity-20"></div>
<div>
<div class="text-[10px] text-on-surface-variant font-bold tracking-wider mb-1">总曝光量</div>
<div class="text-2xl font-extrabold font-headline">124.8k</div>
</div>
</div>
<button class="flex items-center gap-2 bg-surface-container-lowest py-3 px-6 rounded-lg text-sm font-bold shadow-sm hover:shadow-md transition-all active:scale-95">
<span class="material-symbols-outlined text-primary" data-icon="download">download</span>
                    导出完整报告
                </button>
</div>
</div>
</main>
</body></html>
```
