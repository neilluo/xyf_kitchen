# 页面：推广历史 (Promotion History)

> 依赖文档：[02-design-system.md](./02-design-system.md)、[03-shared-infrastructure.md](./03-shared-infrastructure.md)
> 设计稿：`ui/stitch_grace_video_management/grace_promotion_history/screen.png`
> HTML 原型：`ui/stitch_grace_video_management/grace_promotion_history/code.html`
> 路由：`/promotions`

## A. 页面概览

推广历史页面展示所有视频的推广执行记录，支持按视频、日期范围筛选，可展开行查看各渠道详细执行结果。底部有 Insights 卡片展示推广数据洞察。

## B. 组件层级树

```
PromotionHistoryPage
├── PageHeader ("推广历史" 标题 + "导出" 按钮)
├── FilterBar
│   ├── VideoSelect (选择视频下拉)
│   ├── DateRangeInput (日期范围)
│   └── ExportButton (download 图标)
├── PromotionTable
│   ├── TableHeader
│   └── TableBody
│       └── PromotionGroup × N (按视频分组)
│           ├── SummaryRow (可展开)
│           │   ├── ExpandToggle (chevron_right / expand_more)
│           │   ├── VideoTitle
│           │   ├── ChannelCount
│           │   ├── OverallProgress (圆形 SVG 进度)
│           │   ├── StatusBadge
│           │   └── Date
│           └── ExpandedDetail (展开后)
│               └── ChannelDetailRow × N
│                   ├── ChannelIcon + ChannelName
│                   ├── MethodBadge
│                   ├── StatusBadge
│                   ├── ResultLink (open_in_new)
│                   └── RetryButton (仅失败时)
├── Pagination
└── InsightCards (3 列 bento 布局)
    ├── InsightCard (成功率, tertiary-fixed)
    ├── InsightCard (最佳方式, secondary-fixed)
    └── InsightCard (高峰时段, surface-container-high)
```

## C. API 端点

| 端点 | 方法 | Hook | 调用时机 |
|------|------|------|----------|
| F3. `/api/promotions/history/{videoId}` | GET | `usePromotionHistory(videoId, params)` | 页面加载、筛选变更 |
| F4. `/api/promotions/report/{videoId}` | GET | `usePromotionReport(videoId)` | 展开行时加载报告 |
| F5. `/api/promotions/{id}/retry` | POST | `useRetryPromotion()` | 点击重试按钮 |
| B5. `/api/videos` | GET | `useVideoList({pageSize:100})` | 加载视频下拉选项 |

### 推广历史响应

```typescript
// PaginatedData<PromotionRecord>
interface PromotionRecord {
  promotionRecordId: string
  videoId: string
  channelId: string
  channelName: string
  channelType: ChannelType
  promotionTitle: string
  promotionBody: string
  method: PromotionMethod
  status: PromotionStatus  // PENDING | EXECUTING | COMPLETED | FAILED
  resultUrl: string | null
  errorMessage: string | null
  executedAt: string | null
  createdAt: string
}
```

### 推广报告响应

```typescript
interface PromotionReport {
  videoId: string
  videoTitle: string
  totalChannels: number
  successCount: number
  failedCount: number
  pendingCount: number
  overallSuccessRate: number  // 0.0-1.0
  channelSummaries: ChannelSummary[]
}

interface ChannelSummary {
  channelId: string
  channelName: string
  channelType: ChannelType
  method: PromotionMethod
  status: PromotionStatus
  resultUrl: string | null
  errorMessage: string | null
  executedAt: string | null
}
```

## D. 状态管理

| 类型 | 内容 | 管理方式 |
|------|------|----------|
| 服务端状态 | 推广历史列表 | React Query `usePromotionHistory` |
| 服务端状态 | 推广报告 | React Query `usePromotionReport` |
| 服务端状态 | 视频列表（下拉选项） | React Query `useVideoList` |
| 本地状态 | 筛选参数 (videoId, dateRange) | `useState` |
| 本地状态 | 展开行 ID 集合 | `useState<Set<string>>` |

## E. 关键交互

| 用户操作 | 触发行为 | 状态变更 | UI 反馈 |
|---------|---------|---------|---------|
| 选择视频筛选 | 更新 videoId 参数 | refetch 历史列表 | 列表刷新 |
| 选择日期范围 | 更新 date 参数 | refetch | 列表刷新 |
| 点击展开/折叠行 | 切换展开状态 | expandedIds 更新 | 行展开/收起动画 |
| 展开行首次加载 | 调用 F4 获取报告 | - | 详情区域渲染 |
| 点击结果链接 | 新窗口打开 URL | - | `window.open(url)` |
| 点击重试按钮 | 调用 F5 重试推广 | 该记录状态更新 | Toast + 状态刷新 |
| 点击导出按钮 | 导出推广数据 | - | 下载 CSV/Excel |
| 分页 | 更新页码 | refetch | 列表刷新 |

## F. 错误处理

| 场景 | 错误码 | 用户提示 | 恢复操作 |
|------|--------|---------|---------|
| 视频不存在 | 1008 | "视频不存在" | - |
| 推广记录不存在 | 4004 | "推广记录不存在" | 刷新列表 |
| 渠道已禁用 | 4003 | "渠道已被禁用，无法重试" | - |
| 重试失败 | 9002 | "推广执行失败" | 允许再次重试 |
| 空列表 | - | "暂无推广记录" | - |

## G. 视觉实现备注

### FilterBar

```
容器：bg-surface-container-low rounded-lg p-4 flex items-center gap-4
视频选择：
  bg-surface-container-lowest rounded-md px-4 py-2.5 text-sm w-64
日期范围：
  bg-surface-container-lowest rounded-md px-4 py-2.5 text-sm
导出按钮：
  ml-auto, bg-surface-container-high text-on-surface rounded-lg px-4 py-2
  flex items-center gap-2, download 图标
```

### PromotionTable

```
容器：bg-surface-container-lowest rounded-lg overflow-hidden mt-6
表头：bg-surface-container-low/50
  th: px-4 py-3 text-xs font-medium text-on-surface-variant uppercase tracking-wider

SummaryRow：
  tr: cursor-pointer hover:bg-surface-bright transition-colors
  展开图标：chevron_right (折叠) / expand_more (展开)
            text-on-surface-variant transition-transform
  视频标题：font-medium text-on-surface
  渠道数：text-on-surface-variant text-sm
  圆形进度 (SVG)：
    w-12 h-12
    底圈：stroke-surface-container-highest stroke-width-3
    进度圈：stroke-primary stroke-width-3
            stroke-dasharray/stroke-dashoffset 计算
    中心文字：text-xs font-bold "80%"
  状态：StatusBadge
    EXECUTING 状态特殊处理：
      bg-secondary-fixed + animate-pulse 圆点
      <span class="w-2 h-2 rounded-full bg-primary animate-pulse mr-1.5" />
  日期：text-on-surface-variant text-sm

ExpandedDetail：
  容器：bg-surface-container-low/30 px-8 py-4
  渠道行：flex items-center gap-4 py-3
    渠道图标：w-8 h-8 rounded-full bg-{color}
    渠道名：font-medium text-sm
    方式 badge：bg-surface-container-high px-2 py-0.5 rounded text-xs
    状态：StatusBadge (小号)
    结果链接：text-primary text-sm flex items-center gap-1
              open_in_new 图标 size=14
    重试按钮 (仅失败)：text-primary text-sm hover:underline
                        refresh 图标 size=14 + "重试"
```

### InsightCards (Bento 布局)

```
容器：grid grid-cols-3 gap-6 mt-8

卡片 1 (成功率)：
  bg-tertiary-fixed rounded-lg p-6
  数值：font-headline text-3xl font-bold text-on-tertiary-fixed
  标签：font-body text-sm text-on-tertiary-fixed-variant mt-1
  图标装饰：右上角 trophy 图标 text-tertiary/20 size=48

卡片 2 (最佳方式)：
  bg-secondary-fixed rounded-lg p-6
  数值：font-headline text-xl font-bold text-on-secondary-fixed
  标签：font-body text-sm text-on-secondary-fixed-variant mt-1
  描述："POST 方式效果最佳"

卡片 3 (高峰时段)：
  bg-surface-container-high rounded-lg p-6
  数值：font-headline text-xl font-bold text-on-surface
  标签：font-body text-sm text-on-surface-variant mt-1
  描述："上午 10:00 - 12:00 发布效果最好"
```


## H. HTML 原型参考代码

> 此 HTML 原型使用 CDN Tailwind CSS 构建，包含页面的完整 DOM 结构和精确的 Tailwind class。开发时请参考其中的布局结构、颜色 token、间距和组件样式。

> 源文件：`ui/stitch_grace_video_management/grace_promotion_history/code.html`

```html
<!DOCTYPE html>

<html class="light" lang="zh-CN"><head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
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
        body { font-family: 'Inter', sans-serif; }
        h1, h2, h3 { font-family: 'Manrope', sans-serif; }
        .no-scrollbar::-webkit-scrollbar { display: none; }
    </style>
</head>
<body class="bg-surface text-on-surface min-h-screen">
<!-- SideNavBar Shell -->
<aside class="bg-[#001529] h-screen w-[240px] fixed left-0 top-0 flex flex-col justify-between pb-8 z-20 shadow-[0_8px_32px_rgba(0,26,67,0.06)]">
<div>
<div class="py-8 px-6 flex items-center gap-3">
<div class="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
<span class="material-symbols-outlined text-white text-xl" data-icon="restaurant_menu">restaurant_menu</span>
</div>
<div>
<div class="text-white text-xl font-bold font-headline leading-none">Grace</div>
<div class="text-slate-500 text-[10px] tracking-wider uppercase mt-1">Video Distribution</div>
</div>
</div>
<nav class="mt-4 space-y-1">
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-4 hover:bg-white/5 group" href="#">
<span class="material-symbols-outlined group-hover:scale-110 transition-transform" data-icon="dashboard">dashboard</span>
<span class="font-body text-sm">仪表盘</span>
</a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-4 hover:bg-white/5 group" href="#">
<span class="material-symbols-outlined group-hover:scale-110 transition-transform" data-icon="video_library">video_library</span>
<span class="font-body text-sm">视频管理</span>
</a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-4 hover:bg-white/5 group" href="#">
<span class="material-symbols-outlined group-hover:scale-110 transition-transform" data-icon="fact_check">fact_check</span>
<span class="font-body text-sm">元数据审核</span>
</a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-4 hover:bg-white/5 group" href="#">
<span class="material-symbols-outlined group-hover:scale-110 transition-transform" data-icon="publish">publish</span>
<span class="font-body text-sm">视频发布</span>
</a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-4 hover:bg-white/5 group" href="#">
<span class="material-symbols-outlined group-hover:scale-110 transition-transform" data-icon="alt_route">alt_route</span>
<span class="font-body text-sm">推广渠道</span>
</a>
<a class="bg-blue-600/10 text-blue-400 border-r-4 border-blue-500 py-4 px-6 flex items-center gap-4" href="#">
<span class="material-symbols-outlined" data-icon="task">task</span>
<span class="font-body text-sm">推广任务</span>
</a>
</nav>
</div>
<div class="px-6">
<a class="text-slate-400 hover:text-white py-4 flex items-center gap-4 transition-colors group" href="#">
<span class="material-symbols-outlined group-hover:rotate-45 transition-transform" data-icon="settings">settings</span>
<span class="font-body text-sm">设置</span>
</a>
</div>
</aside>
<!-- TopNavBar Shell -->
<header class="h-16 fixed top-0 right-0 left-[240px] w-[calc(100%-240px)] z-10 bg-white/80 backdrop-blur-md flex items-center justify-between px-8">
<div class="flex items-center gap-2 text-slate-500 font-inter text-sm">
<span>Grace Platform</span>
<span class="material-symbols-outlined text-xs" data-icon="chevron_right">chevron_right</span>
<span class="text-blue-600 font-semibold">推广历史</span>
</div>
<div class="flex items-center gap-6">
<button class="relative text-slate-500 hover:bg-slate-50 p-2 rounded-lg transition-all">
<span class="material-symbols-outlined" data-icon="notifications">notifications</span>
<span class="absolute top-2 right-2 w-2 h-2 bg-error rounded-full border-2 border-white"></span>
</button>
<div class="flex items-center gap-3 pl-4 border-l border-surface-container-high">
<img alt="User Avatar" class="w-8 h-8 rounded-full object-cover" data-alt="Professional headshot of a creative director in a minimalist studio setting with soft natural lighting" src="https://lh3.googleusercontent.com/aida-public/AB6AXuASmpNHtoJUqFtXCIY4OXe5GB_FZLI1mzix7G8hPZCephbX08UgwZYrMl7iJOP9D8wwAigB4MTlL5m-p7ByIP4OPVkgPNTJn4fTMyymgO7gkHd9TlB-5rOSd33V2DxLikJoFRU8LHsYMlcg1mnP0X9G0rmR-ncjugPkts8UpYdfzgxbP8KeUlido_jThwobkxOZDPKGwtJFgiUGX6aAWhJJ0awXIXYSlASpGSt9tao7oc_1BtmqHfj63t90t6APVO1EK0f104SIscfi"/>
<span class="text-sm font-medium text-on-surface">Grace Chen</span>
</div>
</div>
</header>
<!-- Main Canvas -->
<main class="ml-[240px] pt-24 px-8 pb-12">
<!-- Page Header & Filters -->
<div class="flex flex-col md:flex-row md:items-end justify-between gap-6 mb-10">
<div>
<h1 class="text-4xl font-extrabold font-headline text-on-surface tracking-tight">推广历史</h1>
<p class="text-on-surface-variant mt-2 font-body">监控和分析您在所有渠道的视频推广效果。</p>
</div>
<div class="flex flex-wrap items-center gap-3">
<div class="relative group">
<select class="appearance-none bg-surface-container-low border-none rounded-lg px-4 pr-10 py-2.5 text-sm font-medium focus:ring-2 focus:ring-primary/40 transition-all cursor-pointer min-w-[200px]">
<option>所有视频</option>
<option>夏日清爽意面教学</option>
<option>自制法式马卡龙全攻略</option>
</select>
<span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-slate-500" data-icon="expand_more">expand_more</span>
</div>
<div class="flex items-center bg-surface-container-low rounded-lg px-4 py-2.5 gap-3">
<span class="material-symbols-outlined text-slate-500 text-sm" data-icon="calendar_today">calendar_today</span>
<input class="bg-transparent border-none p-0 text-sm font-medium focus:ring-0 w-[180px]" readonly="" type="text" value="2024-03-01 - 2024-03-20"/>
</div>
<button class="bg-primary hover:bg-primary-container text-white px-5 py-2.5 rounded-lg text-sm font-semibold flex items-center gap-2 transition-all shadow-sm active:scale-95">
<span class="material-symbols-outlined text-sm" data-icon="download">download</span>
                    导出报告
                </button>
</div>
</div>
<!-- Promotion Records Table -->
<div class="bg-surface-container-lowest rounded-xl overflow-hidden shadow-[0_8px_32px_rgba(0,26,67,0.06)]">
<div class="overflow-x-auto">
<table class="w-full text-left border-collapse">
<thead>
<tr class="bg-surface-container-low/50">
<th class="px-6 py-4 text-xs font-bold text-on-surface-variant uppercase tracking-wider">视频</th>
<th class="px-6 py-4 text-xs font-bold text-on-surface-variant uppercase tracking-wider">渠道</th>
<th class="px-6 py-4 text-xs font-bold text-on-surface-variant uppercase tracking-wider">方式</th>
<th class="px-6 py-4 text-xs font-bold text-on-surface-variant uppercase tracking-wider">状态</th>
<th class="px-6 py-4 text-xs font-bold text-on-surface-variant uppercase tracking-wider">结果链接</th>
<th class="px-6 py-4 text-xs font-bold text-on-surface-variant uppercase tracking-wider">时间</th>
<th class="px-6 py-4 text-xs font-bold text-on-surface-variant uppercase tracking-wider">操作</th>
</tr>
</thead>
<tbody class="divide-y divide-surface-container-low">
<!-- Row 1: Active/Selected -->
<tr class="bg-surface-bright group hover:bg-surface-container-low/30 transition-colors">
<td class="px-6 py-4">
<div class="flex items-center gap-4">
<div class="w-16 h-10 rounded overflow-hidden flex-shrink-0 bg-surface-container-high relative">
<img alt="Video Thumbnail" class="w-full h-full object-cover" data-alt="Cinematic close-up of vibrant Mediterranean pasta dish with fresh basil and olive oil in a rustic kitchen setting" src="https://lh3.googleusercontent.com/aida-public/AB6AXuCFuUbB7Xq8qgT4Z-o-Dd-NgwSURMAIyZhfMDIkalBdxOGYYJsMP_VHtL-0t1DhyfiYQzIhLYLB-sXEYAWtSCI2fkUFpKmpl8VSEY7nVd68p2b8t9y6f4IxdcwSIKfNKxc425C_3kVeURcDyZtFLfBqIKa0bqmIyBLIMwnosAErf1tsxlS7KSJ3htUr68Wftn5Cf6UO2DnaqqcKzO0LVOiJzdGRzD6yxijnEDMyU65xCvNETI4O3Dn9KE3yS2bFNV0Ak9Jrf4k1coH6"/>
<div class="absolute inset-0 flex items-center justify-center bg-black/20 opacity-0 group-hover:opacity-100 transition-opacity">
<span class="material-symbols-outlined text-white text-lg" data-icon="play_arrow" data-weight="fill">play_arrow</span>
</div>
</div>
<span class="text-sm font-semibold text-on-surface truncate max-w-[180px]">夏日清爽意面教学</span>
</div>
</td>
<td class="px-6 py-4">
<div class="flex items-center gap-2">
<div class="w-6 h-6 rounded-full bg-[#FF0000] flex items-center justify-center text-[10px] text-white">YT</div>
<span class="text-sm font-medium">YouTube</span>
</div>
</td>
<td class="px-6 py-4">
<span class="px-2 py-1 bg-surface-container-high rounded text-[10px] font-bold text-on-surface-variant uppercase tracking-tight">POST</span>
</td>
<td class="px-6 py-4">
<span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-secondary-fixed text-on-secondary-fixed">
<span class="w-1.5 h-1.5 rounded-full bg-primary animate-pulse"></span>
                                    进行中
                                </span>
</td>
<td class="px-6 py-4 text-sm text-outline">等待生成...</td>
<td class="px-6 py-4 text-sm text-on-surface-variant font-medium">2026-03-20 14:30</td>
<td class="px-6 py-4">
<button class="text-primary hover:bg-primary/5 p-2 rounded-lg transition-colors">
<span class="material-symbols-outlined" data-icon="visibility">visibility</span>
</button>
</td>
</tr>
<!-- Row 2: Expanded Report -->
<tr class="bg-surface-container-lowest">
<td class="p-0" colspan="7">
<div class="bg-surface-container-low/30 border-y border-surface-container-high p-8">
<div class="max-w-4xl mx-auto flex flex-col md:flex-row gap-12 items-center">
<!-- Circular Progress Section -->
<div class="flex flex-col items-center gap-4 text-center">
<div class="relative w-32 h-32 flex items-center justify-center">
<svg class="w-full h-full -rotate-90">
<circle class="text-surface-container-high" cx="64" cy="64" fill="transparent" r="58" stroke="currentColor" stroke-width="8"></circle>
<circle class="text-primary" cx="64" cy="64" fill="transparent" r="58" stroke="currentColor" stroke-dasharray="364.4" stroke-dashoffset="72.8" stroke-width="8"></circle>
</svg>
<div class="absolute inset-0 flex flex-col items-center justify-center">
<span class="text-3xl font-extrabold font-headline text-on-surface">80%</span>
</div>
</div>
<div>
<h4 class="text-lg font-bold text-on-surface">4/5 渠道推广成功</h4>
<p class="text-sm text-on-surface-variant mt-1">综合推广效能评估：优秀</p>
</div>
</div>
<!-- Channels Detail Section -->
<div class="flex-1 w-full space-y-5">
<div class="space-y-2">
<div class="flex justify-between items-end mb-1">
<span class="text-xs font-bold text-on-surface-variant uppercase tracking-wide">YouTube (Global)</span>
<span class="text-xs font-bold text-green-600">SUCCESS</span>
</div>
<div class="h-2 w-full bg-surface-container-highest rounded-full overflow-hidden">
<div class="h-full bg-green-500 w-full"></div>
</div>
</div>
<div class="space-y-2">
<div class="flex justify-between items-end mb-1">
<span class="text-xs font-bold text-on-surface-variant uppercase tracking-wide">Instagram (Foodie Ads)</span>
<span class="text-xs font-bold text-green-600">SUCCESS</span>
</div>
<div class="h-2 w-full bg-surface-container-highest rounded-full overflow-hidden">
<div class="h-full bg-green-500 w-full"></div>
</div>
</div>
<div class="space-y-2">
<div class="flex justify-between items-end mb-1">
<span class="text-xs font-bold text-on-surface-variant uppercase tracking-wide">TikTok (Trend Boost)</span>
<span class="text-xs font-bold text-error">FAILED</span>
</div>
<div class="h-2 w-full bg-surface-container-highest rounded-full overflow-hidden">
<div class="h-full bg-error w-[15%]"></div>
</div>
</div>
<div class="space-y-2">
<div class="flex justify-between items-end mb-1">
<span class="text-xs font-bold text-on-surface-variant uppercase tracking-wide">Facebook (Recipe Groups)</span>
<span class="text-xs font-bold text-green-600">SUCCESS</span>
</div>
<div class="h-2 w-full bg-surface-container-highest rounded-full overflow-hidden">
<div class="h-full bg-green-500 w-full"></div>
</div>
</div>
</div>
</div>
</div>
</td>
</tr>
<!-- Row 3 -->
<tr class="hover:bg-surface-container-low/30 transition-colors">
<td class="px-6 py-4">
<div class="flex items-center gap-4">
<div class="w-16 h-10 rounded overflow-hidden flex-shrink-0 bg-surface-container-high">
<img alt="Video Thumbnail" class="w-full h-full object-cover" data-alt="Top-down view of a colorful pastry chef workstation with flour dust and vibrant macaron shells on a marble surface" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBeyKKbGXxYLnFz-n3pTQWkJQHrsAjta4yVMFofBjMdFxnCJX4IXY9bZx99KgxGZCcbIYv5JffDTdGSmxYYqu-wboTnYST-5B-axNMS4KvdIEcPxmlVzb2mFVFvuT1xOxbMpJllcCM-RfTG4BpaGam-QQ9T8VyzAdqb49iGYHmlwbsfO317qQgv5nTNp4QPjNIFDvziAx48iY2iFFPx5x89aurI4WclSXmWrYJkIka21ckTa8anoVrXEJunxMSEjb-6YUXaoSYbt-qj"/>
</div>
<span class="text-sm font-semibold text-on-surface truncate max-w-[180px]">自制法式马卡龙全攻略</span>
</div>
</td>
<td class="px-6 py-4">
<div class="flex items-center gap-2">
<div class="w-6 h-6 rounded-full bg-[#FE2C55] flex items-center justify-center text-[10px] text-white">TK</div>
<span class="text-sm font-medium">TikTok</span>
</div>
</td>
<td class="px-6 py-4">
<span class="px-2 py-1 bg-surface-container-high rounded text-[10px] font-bold text-on-surface-variant uppercase tracking-tight">COMMENT</span>
</td>
<td class="px-6 py-4">
<span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-700">
<span class="material-symbols-outlined text-xs" data-icon="check_circle" data-weight="fill">check_circle</span>
                                    发布成功
                                </span>
</td>
<td class="px-6 py-4">
<a class="text-sm text-primary font-medium hover:underline flex items-center gap-1" href="#">
                                    tiktok.com/v/82k...
                                    <span class="material-symbols-outlined text-xs" data-icon="open_in_new">open_in_new</span>
</a>
</td>
<td class="px-6 py-4 text-sm text-on-surface-variant font-medium">2026-03-19 09:15</td>
<td class="px-6 py-4">
<div class="flex gap-2">
<button class="text-outline hover:text-primary p-2 rounded-lg transition-colors">
<span class="material-symbols-outlined" data-icon="refresh">refresh</span>
</button>
<button class="text-outline hover:text-primary p-2 rounded-lg transition-colors">
<span class="material-symbols-outlined" data-icon="visibility">visibility</span>
</button>
</div>
</td>
</tr>
<!-- Row 4 -->
<tr class="hover:bg-surface-container-low/30 transition-colors">
<td class="px-6 py-4">
<div class="flex items-center gap-4">
<div class="w-16 h-10 rounded overflow-hidden flex-shrink-0 bg-surface-container-high">
<img alt="Video Thumbnail" class="w-full h-full object-cover" data-alt="Close up of artisanal bread with steam rising, warm golden crust, shot in a high-end bakery kitchen" src="https://lh3.googleusercontent.com/aida-public/AB6AXuA9NTXutDkyRHH8gsP5Mt5oBKowzJ29Oty013iWyJMuQtlBRpWG-HH8owuS-YC-oNQ2d-ggxKPtmxiLMNFPy-E0RW4QS5yln5R5PynJxwCzv7taLtSNTz9N-ZMCNnnWjcL83NQ87vcxosc5tsKlHz2Bly_QcPadrx25Hcz0DF2sZ00uAK_SArJgVMBjdx4WV8zlcC76pnar3CenxAD8tGGahhV5210cUAO9CW6ve7gWs-eTodDXOV0inYyv80nEoL5ofso0d1gg-qAQ"/>
</div>
<span class="text-sm font-semibold text-on-surface truncate max-w-[180px]">夏日清爽意面教学</span>
</div>
</td>
<td class="px-6 py-4">
<div class="flex items-center gap-2">
<div class="w-6 h-6 rounded-full bg-[#1877F2] flex items-center justify-center text-[10px] text-white">FB</div>
<span class="text-sm font-medium">Facebook</span>
</div>
</td>
<td class="px-6 py-4">
<span class="px-2 py-1 bg-surface-container-high rounded text-[10px] font-bold text-on-surface-variant uppercase tracking-tight">POST</span>
</td>
<td class="px-6 py-4">
<span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-error-container text-on-error-container">
<span class="material-symbols-outlined text-xs" data-icon="error" data-weight="fill">error</span>
                                    发布失败
                                </span>
</td>
<td class="px-6 py-4 text-sm text-error/60 italic">网络超时</td>
<td class="px-6 py-4 text-sm text-on-surface-variant font-medium">2026-03-18 22:45</td>
<td class="px-6 py-4">
<div class="flex gap-2">
<button class="text-primary hover:bg-primary/5 p-2 rounded-lg transition-colors group">
<span class="material-symbols-outlined group-hover:rotate-180 transition-transform duration-500" data-icon="refresh">refresh</span>
</button>
<button class="text-outline hover:text-primary p-2 rounded-lg transition-colors">
<span class="material-symbols-outlined" data-icon="visibility">visibility</span>
</button>
</div>
</td>
</tr>
</tbody>
</table>
</div>
<!-- Pagination-like Footer -->
<div class="px-6 py-4 flex items-center justify-between bg-surface-container-low/20">
<span class="text-xs font-medium text-on-surface-variant">显示 1 - 4 条记录 (共 128 条)</span>
<div class="flex gap-2">
<button class="w-8 h-8 rounded-lg flex items-center justify-center bg-surface-container-lowest text-outline hover:bg-primary hover:text-white transition-all shadow-sm">
<span class="material-symbols-outlined text-sm" data-icon="chevron_left">chevron_left</span>
</button>
<button class="w-8 h-8 rounded-lg flex items-center justify-center bg-primary text-white shadow-md font-bold text-xs">1</button>
<button class="w-8 h-8 rounded-lg flex items-center justify-center bg-surface-container-lowest text-on-surface-variant hover:bg-primary/10 transition-all text-xs font-bold">2</button>
<button class="w-8 h-8 rounded-lg flex items-center justify-center bg-surface-container-lowest text-on-surface-variant hover:bg-primary/10 transition-all text-xs font-bold">3</button>
<button class="w-8 h-8 rounded-lg flex items-center justify-center bg-surface-container-lowest text-outline hover:bg-primary hover:text-white transition-all shadow-sm">
<span class="material-symbols-outlined text-sm" data-icon="chevron_right">chevron_right</span>
</button>
</div>
</div>
</div>
<!-- Secondary Insights (Bento Style) -->
<div class="grid grid-cols-1 md:grid-cols-3 gap-6 mt-10">
<div class="bg-tertiary-fixed rounded-xl p-6 flex flex-col justify-between h-[160px]">
<div class="flex justify-between items-start">
<span class="material-symbols-outlined text-on-tertiary-fixed" data-icon="auto_awesome">auto_awesome</span>
<span class="text-[10px] font-bold px-2 py-0.5 bg-on-tertiary-fixed/10 rounded-full text-on-tertiary-fixed uppercase">Insight</span>
</div>
<div>
<div class="text-2xl font-extrabold text-on-tertiary-fixed">92%</div>
<div class="text-sm font-medium text-on-tertiary-fixed/80">本月平均发布成功率</div>
</div>
</div>
<div class="bg-secondary-fixed rounded-xl p-6 flex flex-col justify-between h-[160px]">
<div class="flex justify-between items-start">
<span class="material-symbols-outlined text-on-secondary-fixed" data-icon="trending_up">trending_up</span>
<span class="text-[10px] font-bold px-2 py-0.5 bg-on-secondary-fixed/10 rounded-full text-on-secondary-fixed uppercase">Method</span>
</div>
<div>
<div class="text-2xl font-extrabold text-on-secondary-fixed">POST</div>
<div class="text-sm font-medium text-on-secondary-fixed/80">最受欢迎的推广方式</div>
</div>
</div>
<div class="bg-surface-container-high rounded-xl p-6 flex flex-col justify-between h-[160px]">
<div class="flex justify-between items-start text-on-surface-variant">
<span class="material-symbols-outlined" data-icon="schedule">schedule</span>
<span class="text-[10px] font-bold px-2 py-0.5 bg-on-surface/5 rounded-full uppercase">Peak Time</span>
</div>
<div>
<div class="text-2xl font-extrabold text-on-surface">14:00 - 16:00</div>
<div class="text-sm font-medium text-on-surface-variant">用户互动最频繁时段</div>
</div>
</div>
</div>
</main>
</body></html>
```
