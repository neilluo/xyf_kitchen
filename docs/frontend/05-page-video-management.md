# 页面：视频管理 (Video Management)

> 依赖文档：[02-design-system.md](./02-design-system.md)、[03-shared-infrastructure.md](./03-shared-infrastructure.md)
> 设计稿：`ui/stitch_grace_video_management/grace_video_management/screen.png`
> HTML 原型：`ui/stitch_grace_video_management/grace_video_management/code.html`
> 路由：`/videos`

## A. 页面概览

视频管理页展示所有已上传视频的列表，支持搜索、状态筛选、日期范围筛选和分页。用户可从此页面跳转到元数据审核、视频分发等操作。

## B. 组件层级树

```
VideoManagementPage
├── PageHeader ("视频管理" 标题 + "上传新视频" 按钮)
├── FilterBar
│   ├── SearchInput (keyword, 带 search 图标)
│   ├── StatusSelect (状态下拉筛选)
│   └── DateRangePicker (日期范围)
├── VideoTable
│   ├── TableHeader
│   └── TableBody
│       └── VideoRow × N
│           ├── Thumbnail (w-[80px] h-[45px], hover 播放叠层)
│           ├── FileName + Format badge
│           ├── Duration
│           ├── FileSize
│           ├── UploadDate
│           ├── StatusBadge
│           └── ActionButtons
│               ├── IconButton (visibility - 查看)
│               ├── IconButton (edit - 编辑元数据)
│               ├── IconButton (send - 分发)
│               └── IconButton (more_vert - 更多)
└── Pagination
```

## C. API 端点

| 端点 | 方法 | Hook | 调用时机 |
|------|------|------|----------|
| B5. `/api/videos` | GET | `useVideoList(params)` | 页面加载、筛选变更、翻页 |
| B6. `/api/videos/{videoId}` | GET | `useVideoDetail(id)` | 查看详情（可选，用于弹窗） |

### 查询参数

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| page | number | 1 | 页码 |
| pageSize | number | 20 | 每页条数 |
| keyword | string | - | 按文件名搜索 |
| status | string | - | 状态筛选（逗号分隔多值） |
| startDate | string | - | 起始日期 |
| endDate | string | - | 截止日期 |
| sort | string | `createdAt` | 排序字段 |
| order | string | `desc` | 排序方向 |

### 响应数据

```typescript
// PaginatedData<Video>
interface Video {
  videoId: string
  fileName: string
  format: VideoFormat
  fileSize: number
  duration: string
  status: VideoStatus
  thumbnailUrl: string | null
  hasMetadata: boolean
  createdAt: string
  updatedAt: string
}
```

## D. 状态管理

| 类型 | 内容 | 管理方式 |
|------|------|----------|
| 服务端状态 | 视频列表（分页） | React Query `useVideoList` |
| 本地状态 | 筛选参数 | `useState<VideoListParams>` |
| 本地状态 | 当前页码 | 包含在筛选参数中 |

## E. 关键交互

| 用户操作 | 触发行为 | 状态变更 | UI 反馈 |
|---------|---------|---------|---------|
| 输入搜索关键词 | 防抖 300ms 后更新 keyword | 重置 page=1，refetch | 列表刷新 |
| 选择状态筛选 | 更新 status 参数 | 重置 page=1，refetch | 列表刷新 |
| 选择日期范围 | 更新 startDate/endDate | 重置 page=1，refetch | 列表刷新 |
| 点击分页按钮 | 更新 page 参数 | refetch | 列表刷新 |
| 点击 visibility 按钮 | 查看视频详情 | - | 弹窗或跳转 |
| 点击 edit 按钮 | 导航到元数据审核 | `navigate(/videos/${id}/metadata)` | 页面跳转 |
| 点击 send 按钮 | 导航到分发页面 | `navigate(/videos/${id}/distribute)` | 页面跳转 |
| 点击"上传新视频"按钮 | 导航到上传页 | `navigate(/upload)` | 页面跳转 |
| 缩略图 hover | 显示播放图标叠层 | - | `play_arrow` 图标浮现 |

## F. 错误处理

| 场景 | 错误码 | 用户提示 | 恢复操作 |
|------|--------|---------|---------|
| 列表加载失败 | - | "加载视频列表失败" | 重试按钮 |
| 空列表 | - | "暂无视频，去上传第一个视频吧" | 显示上传链接 |

## G. 视觉实现备注

### FilterBar

```
容器：bg-surface-container-low rounded-lg p-4 flex items-center gap-4
搜索框：
  容器：relative flex items-center
  图标：absolute left-3, search 图标, text-on-surface-variant
  输入：pl-10 pr-4 py-2.5 bg-surface-container-lowest rounded-md
        w-64 text-sm placeholder:text-on-surface-variant/50
状态下拉：
  bg-surface-container-lowest rounded-md px-4 py-2.5 text-sm
  带 chevron_down 图标
日期范围：
  类似输入框样式，带日历图标
```

### VideoTable

```
表容器：bg-surface-container-lowest rounded-lg overflow-hidden
表头：bg-surface-container-low/50
  th: px-4 py-3 text-xs font-medium text-on-surface-variant uppercase tracking-wider text-left
表行：
  tr: hover:bg-surface-bright transition-colors
  td: px-4 py-4 text-sm
缩略图列：
  容器：w-[80px] h-[45px] rounded overflow-hidden relative
  图片：object-cover w-full h-full
  hover 叠层：absolute inset-0 bg-black/40 flex items-center justify-center
              play_arrow 图标 text-white
              opacity-0 group-hover:opacity-100 transition-opacity
文件名：font-medium text-on-surface
格式 badge：ml-2 text-xs bg-surface-container-high px-1.5 py-0.5 rounded
时长/大小/日期：text-on-surface-variant
状态：StatusBadge 组件（见 02-design-system.md）
操作按钮：
  容器：flex items-center gap-1
  按钮：p-2 rounded-full hover:bg-primary/10 text-on-surface-variant
        hover:text-primary transition-colors
  图标：visibility, edit, send, more_vert (size=18)
```

### Pagination

```
容器：flex items-center justify-between px-4 py-4
左侧：text-sm text-on-surface-variant "共 {total} 个视频"
右侧：flex items-center gap-2
  箭头按钮：w-8 h-8 flex items-center justify-center rounded-lg
            text-on-surface-variant hover:bg-surface-container-low
            disabled:opacity-30
  页码按钮：w-8 h-8 flex items-center justify-center rounded-lg text-sm
            默认：text-on-surface-variant hover:bg-surface-container-low
            激活：bg-primary text-white
```


## H. HTML 原型参考代码

> 此 HTML 原型使用 CDN Tailwind CSS 构建，包含页面的完整 DOM 结构和精确的 Tailwind class。开发时请参考其中的布局结构、颜色 token、间距和组件样式。

> 源文件：`ui/stitch_grace_video_management/grace_video_management/code.html`

```html
<!DOCTYPE html>

<html class="light" lang="zh-CN"><head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<title>视频管理 - Grace Platform</title>
<script src="https://cdn.tailwindcss.com?plugins=forms,container-queries"></script>
<link href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;700;800&amp;family=Inter:wght@300;400;500;600&amp;display=swap" rel="stylesheet"/>
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
            vertical-align: middle;
        }
        body { font-family: 'Inter', sans-serif; }
        h1, h2, h3 { font-family: 'Manrope', sans-serif; }
        .no-scrollbar::-webkit-scrollbar { display: none; }
    </style>
</head>
<body class="bg-surface text-on-surface">
<!-- SideNavBar -->
<aside class="h-screen w-[240px] fixed left-0 top-0 bg-[#001529] flex flex-col justify-between pb-8 z-20 shadow-[0_8px_32px_rgba(0,26,67,0.06)]">
<div>
<div class="px-6 py-8 flex items-center gap-2 text-white text-2xl font-bold font-headline">
<span class="material-symbols-outlined text-blue-400" style="font-size: 32px;">play_circle</span>
                Grace
            </div>
<nav class="mt-4 flex flex-col gap-1">
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors hover:bg-white/5 flex items-center gap-3" href="#">
<span class="material-symbols-outlined">dashboard</span>
<span class="font-inter text-sm">仪表盘</span>
</a>
<!-- Active Tab: 视频管理 -->
<a class="bg-blue-600/10 text-blue-400 border-r-4 border-blue-500 py-4 px-6 flex items-center gap-3" href="#">
<span class="material-symbols-outlined">video_library</span>
<span class="font-inter text-sm">视频管理</span>
</a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors hover:bg-white/5 flex items-center gap-3" href="#">
<span class="material-symbols-outlined">fact_check</span>
<span class="font-inter text-sm">元数据审核</span>
</a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors hover:bg-white/5 flex items-center gap-3" href="#">
<span class="material-symbols-outlined">publish</span>
<span class="font-inter text-sm">视频发布</span>
</a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors hover:bg-white/5 flex items-center gap-3" href="#">
<span class="material-symbols-outlined">alt_route</span>
<span class="font-inter text-sm">推广渠道</span>
</a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors hover:bg-white/5 flex items-center gap-3" href="#">
<span class="material-symbols-outlined">task</span>
<span class="font-inter text-sm">推广任务</span>
</a>
</nav>
</div>
<div class="flex flex-col gap-1">
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors hover:bg-white/5 flex items-center gap-3" href="#">
<span class="material-symbols-outlined">settings</span>
<span class="font-inter text-sm">设置</span>
</a>
</div>
</aside>
<!-- TopNavBar -->
<header class="h-16 fixed top-0 right-0 left-[240px] w-[calc(100%-240px)] z-10 bg-white/80 backdrop-blur-md flex items-center justify-between px-8 bg-surface-container-low">
<div class="flex items-center gap-2 text-sm font-inter text-slate-500">
<span>主页</span>
<span class="material-symbols-outlined text-xs">chevron_right</span>
<span class="text-blue-600 font-semibold">视频管理</span>
</div>
<div class="flex items-center gap-6">
<button class="relative text-slate-500 hover:bg-slate-50 p-2 rounded-lg transition-all">
<span class="material-symbols-outlined">notifications</span>
<span class="absolute top-2 right-2 w-2 h-2 bg-error rounded-full"></span>
</button>
<div class="flex items-center gap-3 pl-4 border-l border-outline-variant/30">
<div class="text-right">
<p class="text-xs font-bold text-on-surface">林小厨</p>
<p class="text-[10px] text-slate-400">内容创作者</p>
</div>
<img alt="User Avatar" class="w-8 h-8 rounded-full bg-surface-container-high object-cover" data-alt="professional headshot of a smiling asian woman content creator with soft natural lighting" src="https://lh3.googleusercontent.com/aida-public/AB6AXuDsS9SOJB-RpHKduhJWs6MB5g_7d99dyD6kbkKw9OO40Fya7nno3QGzSCwAU7-nl_goyw1WQqLDoBos9i5kWdGvw9M86GKdQPCum1dy-2UyKfSSEdmS80OH34ADXoC2-Kx6wFg-X1joeU1VLUxbdFavRYTnfHBiPWk2Rnq0vtK9XIFA8RpPWtQxLMsqCXOgrD5kQ6_xORUcariky2D5gAUvCjzIrbNx2slFjB0MM9cex7SN6YZXra6QL4zeHt_JZ-kZqzIO4WNcyNl8"/>
</div>
</div>
</header>
<!-- Main Content Canvas -->
<main class="ml-[240px] pt-16 min-h-screen">
<div class="p-8 max-w-7xl mx-auto">
<!-- Page Header -->
<div class="flex items-end justify-between mb-10">
<div>
<h1 class="text-[2.75rem] font-extrabold font-headline leading-tight tracking-tight text-on-surface">视频管理</h1>
<p class="text-body-md text-slate-500 mt-2">管理所有已上传的视频及其分发状态</p>
</div>
<div class="flex gap-3">
<button class="flex items-center gap-2 px-5 py-2.5 bg-surface-container-high text-on-surface font-medium rounded-lg hover:bg-surface-container-highest transition-all active:scale-95">
<span class="material-symbols-outlined">refresh</span>
                        刷新
                    </button>
<button class="flex items-center gap-2 px-6 py-2.5 bg-gradient-to-r from-primary to-primary-container text-white font-semibold rounded-lg shadow-lg shadow-primary/20 hover:shadow-xl hover:opacity-90 transition-all active:scale-95">
<span class="material-symbols-outlined">add</span>
                        上传视频
                    </button>
</div>
</div>
<!-- Filter Bar -->
<div class="bg-surface-container-low p-4 rounded-xl flex flex-wrap items-center gap-4 mb-6">
<div class="flex-1 min-w-[300px] relative">
<span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">search</span>
<input class="w-full bg-surface-container-lowest border-none rounded-lg py-2.5 pl-10 pr-4 text-sm focus:ring-2 focus:ring-primary/40 transition-all placeholder:text-slate-400" placeholder="搜索视频文件名..." type="text"/>
</div>
<div class="flex items-center gap-4">
<select class="bg-surface-container-lowest border-none rounded-lg py-2.5 px-4 text-sm focus:ring-2 focus:ring-primary/40 transition-all text-on-surface min-w-[140px]">
<option>全部状态</option>
<option>已上传</option>
<option>元数据已生成</option>
<option>待发布</option>
<option>已发布</option>
<option>发布失败</option>
</select>
<div class="bg-surface-container-lowest flex items-center rounded-lg overflow-hidden border-none focus-within:ring-2 focus-within:ring-primary/40 transition-all">
<span class="material-symbols-outlined pl-3 text-slate-400 text-sm">calendar_today</span>
<input class="bg-transparent border-none py-2.5 px-3 text-sm focus:ring-0 placeholder:text-slate-400" placeholder="选择日期范围" type="text"/>
</div>
</div>
</div>
<!-- Data Table Card -->
<div class="bg-surface-container-lowest rounded-xl overflow-hidden shadow-sm">
<table class="w-full text-left border-collapse">
<thead class="bg-surface-container-low/50">
<tr>
<th class="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">预览</th>
<th class="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">文件名</th>
<th class="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">时长</th>
<th class="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">状态</th>
<th class="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider">上传时间</th>
<th class="px-6 py-4 text-xs font-semibold text-slate-500 uppercase tracking-wider text-right">操作</th>
</tr>
</thead>
<tbody class="divide-y divide-outline-variant/10">
<!-- Row 1 -->
<tr class="hover:bg-surface-bright transition-colors group">
<td class="px-6 py-4">
<div class="w-[80px] h-[45px] rounded bg-slate-200 overflow-hidden relative">
<img alt="Thumbnail" class="w-full h-full object-cover" data-alt="vibrant overhead shot of a healthy poke bowl with salmon and avocado in soft daylight" src="https://lh3.googleusercontent.com/aida-public/AB6AXuAFl3w3bM37MMOxPTsPfB5QVNiDn-DaqhdU6pemhkaoNjuEza1Dc8bMFsHGK6HbmadG-djtUrizvwQ3tNGWIT5nslID1fsZXwZs9f9xjMf8-hAPV9Ta9Oszk_eCHDfqg2GpLhTTNLX6EXQj44ZUyNlWCdV9o9Gg5zmvgOZZ31TbC227ObEsyea4fqhGrJYZAt8TiaSa7hbYJWSNQ1_fZzZNCVbZuuyOljS0ff5uQtkQh-wVYgnOkxrP3-bP_bG9c89R-IYdZwokEuBJ"/>
<div class="absolute inset-0 bg-black/20 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
<span class="material-symbols-outlined text-white text-lg">play_arrow</span>
</div>
</div>
</td>
<td class="px-6 py-4">
<div>
<p class="text-sm font-bold text-on-surface">夏日清爽沙拉制作教程.mp4</p>
<p class="text-[11px] text-slate-400 mt-0.5">MP4 · 1.2 GB</p>
</div>
</td>
<td class="px-6 py-4 text-sm text-slate-600">12:34</td>
<td class="px-6 py-4">
<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-secondary-fixed text-on-secondary-fixed-variant uppercase">
                                    已发布
                                </span>
</td>
<td class="px-6 py-4 text-sm text-slate-500">2026-03-20 14:30</td>
<td class="px-6 py-4 text-right">
<div class="flex items-center justify-end gap-2 opacity-60 group-hover:opacity-100 transition-opacity">
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">visibility</span>
</button>
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">edit</span>
</button>
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">send</span>
</button>
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">more_vert</span>
</button>
</div>
</td>
</tr>
<!-- Row 2 -->
<tr class="hover:bg-surface-bright transition-colors group">
<td class="px-6 py-4">
<div class="w-[80px] h-[45px] rounded bg-slate-200 overflow-hidden relative">
<img alt="Thumbnail" class="w-full h-full object-cover" data-alt="rustic wooden kitchen table with fresh pasta flour and rolling pin in moody side lighting" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBdyVk8Ezr6ygGIZSGwtg-AFmnP_1jZniV5ukinhFeHXiaBzXHiO6VVHpCRtHXuTtzPzkQB6olpZ8rmN9yEiQW7G3C9UiNaOiEkmGdeGuGh39YuD8s9wR04ArTn9LuNZ8uO1RjhRD4ESt2EfWVM7iHWdNiMPRfdilBrFimkZ2JKHfKZyaf6YvFHNLqbgLAcGrAJVQun-OKcC4rOptgygPsys8wM2zQDSaNTziepJitLLeKgcVFxUy3n6xsyB1NPTpIiv_oD3SMcBCfs"/>
<div class="absolute inset-0 bg-black/20 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
<span class="material-symbols-outlined text-white text-lg">play_arrow</span>
</div>
</div>
</td>
<td class="px-6 py-4">
<div>
<p class="text-sm font-bold text-on-surface">手擀面深度解析技巧.mov</p>
<p class="text-[11px] text-slate-400 mt-0.5">MOV · 2.5 GB</p>
</div>
</td>
<td class="px-6 py-4 text-sm text-slate-600">08:45</td>
<td class="px-6 py-4">
<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-tertiary-fixed text-on-tertiary-fixed-variant uppercase">
                                    待发布
                                </span>
</td>
<td class="px-6 py-4 text-sm text-slate-500">2026-03-20 12:15</td>
<td class="px-6 py-4 text-right">
<div class="flex items-center justify-end gap-2 opacity-60 group-hover:opacity-100 transition-opacity">
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">visibility</span>
</button>
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">edit</span>
</button>
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">send</span>
</button>
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">more_vert</span>
</button>
</div>
</td>
</tr>
<!-- Row 3 -->
<tr class="hover:bg-surface-bright transition-colors group">
<td class="px-6 py-4">
<div class="w-[80px] h-[45px] rounded bg-slate-200 overflow-hidden relative">
<img alt="Thumbnail" class="w-full h-full object-cover" data-alt="close-up of a perfectly grilled steak with herbs and butter on a dark plate" src="https://lh3.googleusercontent.com/aida-public/AB6AXuCZkF8QHr10SqSQ7fzmEwChhqTaiHUXUOvy01LZiluAEr7LKwCQyMBlcODD8hXynT8wAGEFF1yHDWhVy2_izeFwoRbk5DwY1TMe1_0nOmh6RlchpFt0klVM51vlhnYs5xAh435dLra5WFzbQMnyU-ss4bScRPyOWMxDMYC7AhslM6FGllD7rTo2JYUAXeWskqXiGCUCFIp6KaI5BATPnRObPwaPoALgBMG6zx6aHgM8tEE2OQUFpVnOI_drgF6U3EZnKTZKm5_VtKyh"/>
<div class="absolute inset-0 bg-black/20 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
<span class="material-symbols-outlined text-white text-lg">play_arrow</span>
</div>
</div>
</td>
<td class="px-6 py-4">
<div>
<p class="text-sm font-bold text-on-surface">秘制黑椒牛排的做法.mp4</p>
<p class="text-[11px] text-slate-400 mt-0.5">MP4 · 850 MB</p>
</div>
</td>
<td class="px-6 py-4 text-sm text-slate-600">05:20</td>
<td class="px-6 py-4">
<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-error-container text-on-error-container uppercase">
                                    发布失败
                                </span>
</td>
<td class="px-6 py-4 text-sm text-slate-500">2026-03-19 18:00</td>
<td class="px-6 py-4 text-right">
<div class="flex items-center justify-end gap-2 opacity-60 group-hover:opacity-100 transition-opacity">
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">visibility</span>
</button>
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">edit</span>
</button>
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">send</span>
</button>
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">more_vert</span>
</button>
</div>
</td>
</tr>
<!-- Row 4 -->
<tr class="hover:bg-surface-bright transition-colors group">
<td class="px-6 py-4">
<div class="w-[80px] h-[45px] rounded bg-slate-200 overflow-hidden relative">
<img alt="Thumbnail" class="w-full h-full object-cover" data-alt="assorted fresh mediterranean vegetables in a wicker basket on a sunny countertop" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBhDBWqqXAs4Z-t88R3FG84mlaeyvgj3sD8bsVBpCrqitrqdEYSEg4vq0mpjhOqGpqDsWfOs_JwvYv3yNjNv1V-axqLifx9Kny1-YUMLzg-ybzit_crRis8FqJfeBDklcDrHQaw8-o-PucCI_eXTEeuVlpOAAsIUgcw5vNZWH-1fB36JAhl-H2-90bfoV4Q9xiA8w7slLWDqcQzziWkIrmUPfXOjCTjJPjziFiDsVi0VM8_CVSzXkip_BMSDDQvSEg2TCBUHGTPAjbo"/>
<div class="absolute inset-0 bg-black/20 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
<span class="material-symbols-outlined text-white text-lg">play_arrow</span>
</div>
</div>
</td>
<td class="px-6 py-4">
<div>
<p class="text-sm font-bold text-on-surface">春季时令蔬菜选购指南.mp4</p>
<p class="text-[11px] text-slate-400 mt-0.5">MP4 · 640 MB</p>
</div>
</td>
<td class="px-6 py-4 text-sm text-slate-600">15:10</td>
<td class="px-6 py-4">
<span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-surface-container-high text-on-surface-variant uppercase">
                                    元数据已生成
                                </span>
</td>
<td class="px-6 py-4 text-sm text-slate-500">2026-03-19 15:40</td>
<td class="px-6 py-4 text-right">
<div class="flex items-center justify-end gap-2 opacity-60 group-hover:opacity-100 transition-opacity">
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">visibility</span>
</button>
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">edit</span>
</button>
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">send</span>
</button>
<button class="p-1.5 hover:bg-primary/10 hover:text-primary rounded-md transition-all">
<span class="material-symbols-outlined text-[18px]">more_vert</span>
</button>
</div>
</td>
</tr>
</tbody>
</table>
<!-- Pagination Footer -->
<div class="px-6 py-4 bg-surface-container-lowest border-t border-outline-variant/10 flex items-center justify-between">
<div class="flex items-center gap-4 text-xs text-slate-500">
<span>显示 1-10 共 54 条数据</span>
<div class="flex items-center gap-2">
<span>每页条数:</span>
<select class="bg-transparent border-none py-1 focus:ring-0 text-on-surface font-semibold">
<option>10</option>
<option>20</option>
<option>50</option>
</select>
</div>
</div>
<div class="flex items-center gap-1">
<button class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-surface-container-low text-slate-400">
<span class="material-symbols-outlined text-sm">chevron_left</span>
</button>
<button class="w-8 h-8 flex items-center justify-center rounded-lg bg-primary text-white text-xs font-bold">1</button>
<button class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-surface-container-low text-xs text-on-surface">2</button>
<button class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-surface-container-low text-xs text-on-surface">3</button>
<span class="px-2 text-slate-300">...</span>
<button class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-surface-container-low text-xs text-on-surface">6</button>
<button class="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-surface-container-low text-slate-400">
<span class="material-symbols-outlined text-sm">chevron_right</span>
</button>
</div>
</div>
</div>
</div>
</main>
</body></html>
```
