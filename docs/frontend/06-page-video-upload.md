# 页面：视频上传 (Video Upload)

> 依赖文档：[02-design-system.md](./02-design-system.md)、[03-shared-infrastructure.md](./03-shared-infrastructure.md)
> 设计稿：`ui/stitch_grace_video_management/grace_video_upload/screen.png`
> HTML 原型：`ui/stitch_grace_video_management/grace_video_upload/code.html`
> 路由：`/upload`

## A. 页面概览

视频上传页面提供拖拽上传区域，支持 MP4/MOV/AVI/MKV 格式、最大 5GB。采用分片上传（3 步流程：init → chunk × N → complete），实时展示上传进度、速度和预估时间。上传完成后显示已完成列表。

## B. 组件层级树

```
VideoUploadPage
├── PageHeader ("上传视频" 标题)
├── DropZone
│   ├── DashedBorder (自定义 SVG 虚线边框)
│   ├── UploadIcon (cloud_upload)
│   ├── MainText ("拖拽视频文件到此处")
│   ├── SubText ("或点击选择文件")
│   ├── FormatHint ("支持 MP4、MOV、AVI、MKV，最大 5GB")
│   └── HiddenFileInput
├── UploadProgressSection (上传中时显示)
│   └── UploadProgressCard × N
│       ├── FileInfo (文件名 + 文件大小)
│       ├── ProgressBar
│       ├── ProgressPercent
│       ├── SpeedInfo ("上传速度: 12.5 MB/s | 约 2 分钟")
│       └── CancelButton
├── CompletedUploadsSection
│   └── CompletedUploadItem × N
│       ├── CheckIcon (check_circle, green)
│       ├── FileName
│       ├── FileSize
│       └── ActionLinks ("审核元数据" / "继续上传")
└── EditorialTipCards (2 列)
    ├── TipCard (渐变, 视频格式建议)
    └── TipCard (tertiary-fixed, 最佳实践)
```

## C. API 端点

| 端点 | 方法 | Hook | 调用时机 |
|------|------|------|----------|
| B1. `/api/videos/upload/init` | POST | `useInitUpload()` | 用户选择文件后 |
| B2. `/api/videos/upload/{uploadId}/chunk` | POST | `useUploadChunk()` | 逐片上传（循环调用） |
| B3. `/api/videos/upload/{uploadId}/complete` | POST | `useCompleteUpload()` | 所有分片上传完成后 |
| B4. `/api/videos/upload/{uploadId}/progress` | GET | `useUploadProgress(id)` | 轮询上传进度（备用） |

### 分片上传流程

```
1. 用户选择文件
   ↓
2. POST /api/videos/upload/init
   请求: { fileName, fileSize, format }
   响应: { uploadId, totalChunks, chunkSize, expiresAt }
   ↓
3. 循环 POST /api/videos/upload/{uploadId}/chunk (0..totalChunks-1)
   请求: multipart/form-data { chunkIndex, chunk(binary) }
   响应: { uploadedChunks, totalChunks }
   ↓ 每片完成后更新进度条
4. POST /api/videos/upload/{uploadId}/complete
   响应: { videoId, fileName, status: "UPLOADED" }
   ↓
5. 显示上传完成，提供"审核元数据"链接
```

### 前端分片策略

```typescript
// useUpload hook 核心逻辑
async function uploadFile(file: File) {
  // 1. 从文件名提取格式
  const format = file.name.split('.').pop()?.toUpperCase()

  // 2. 初始化上传
  const { uploadId, totalChunks, chunkSize } = await initUpload({
    fileName: file.name,
    fileSize: file.size,
    format,
  })

  // 3. 逐片上传
  for (let i = 0; i < totalChunks; i++) {
    const start = i * chunkSize
    const end = Math.min(start + chunkSize, file.size)
    const chunk = file.slice(start, end)

    const formData = new FormData()
    formData.append('chunkIndex', String(i))
    formData.append('chunk', chunk)

    await uploadChunk(uploadId, formData)
    // 更新进度: (i + 1) / totalChunks * 100
  }

  // 4. 完成上传
  const result = await completeUpload(uploadId)
  return result // { videoId, ... }
}
```

## D. 状态管理

| 类型 | 内容 | 管理方式 |
|------|------|----------|
| 本地状态 | 当前上传文件列表 + 各自进度 | `useState` 或 Zustand `uploadQueue` |
| 本地状态 | 上传速度、剩余时间 | `useState`（通过计算得出） |
| 本地状态 | 已完成上传列表 | `useState<CompletedUpload[]>` |
| 本地状态 | 拖拽状态 (isDragging) | `useState<boolean>` |

### 速度计算

```typescript
// 记录每片上传的时间，计算滑动窗口平均速度
const startTime = Date.now()
// 上传完一片后:
const elapsed = (Date.now() - startTime) / 1000 // 秒
const uploadedBytes = (chunkIndex + 1) * chunkSize
const speed = uploadedBytes / elapsed // bytes/s
const remaining = (file.size - uploadedBytes) / speed // 秒
```

## E. 关键交互

| 用户操作 | 触发行为 | 状态变更 | UI 反馈 |
|---------|---------|---------|---------|
| 拖拽文件到 DropZone | 触发文件校验 → 开始上传 | isDragging → 上传中 | DropZone 高亮 → 进度卡片出现 |
| 点击 DropZone | 打开文件选择器 | - | 系统文件对话框 |
| 选择不支持的格式 | 客户端校验拦截 | - | Toast 错误提示 |
| 选择超过 5GB 的文件 | 客户端校验拦截 | - | Toast 错误提示 |
| 上传过程中 | 持续更新进度 | progress 递增 | 进度条 + 速度/时间信息更新 |
| 点击取消上传 | 中止当前上传 | 状态变为 cancelled | 进度卡片移除 |
| 上传完成 | 移动到完成列表 | completed 列表新增 | 成功提示 + 完成项显示 |
| 点击"审核元数据" | 导航到元数据审核 | `navigate(/videos/${videoId}/metadata)` | 页面跳转 |

## F. 错误处理

| 场景 | 错误码 | 用户提示 | 恢复操作 |
|------|--------|---------|---------|
| 格式不支持 | 1001 | "不支持的视频格式" | 提示支持的格式 |
| 文件过大 | 1002 | "文件超过 5GB 限制" | - |
| 上传会话过期 | 1004 | "上传会话超时" | 提供"重新上传"按钮 |
| 分片上传失败 | 1005/1006 | "上传出错" | 自动重试当前分片（最多 3 次） |
| 网络中断 | - | "网络中断，等待恢复..." | 自动检测恢复后继续上传 |

### 客户端校验（在调用 API 前）

```typescript
const SUPPORTED_FORMATS = ['mp4', 'mov', 'avi', 'mkv']
const MAX_FILE_SIZE = 5 * 1024 * 1024 * 1024 // 5GB

function validateFile(file: File): string | null {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !SUPPORTED_FORMATS.includes(ext)) {
    return '不支持的视频格式，请上传 MP4、MOV、AVI 或 MKV 文件'
  }
  if (file.size > MAX_FILE_SIZE) {
    return '文件大小超过 5GB 限制'
  }
  return null
}
```

## G. 视觉实现备注

### DropZone

```
容器：relative rounded-xl p-12 text-center cursor-pointer
      bg-[#F0F5FF]/50 (淡蓝色底)
虚线边框：自定义 SVG 实现 dashed border（不使用 CSS border-dashed）
  <svg class="absolute inset-0 w-full h-full">
    <rect rx="12" stroke="#0057c2" stroke-opacity="0.3"
          stroke-width="2" stroke-dasharray="8 4" fill="none"
          width="100%" height="100%" />
  </svg>
拖拽激活：bg-primary/10, 边框 opacity 提高到 0.6
图标：cloud_upload, text-primary, size=48
主文字：font-headline text-lg font-bold text-on-surface mt-4
副文字：font-body text-sm text-on-surface-variant mt-2
格式提示：font-body text-xs text-on-surface-variant/60 mt-4
```

### UploadProgressCard

```
容器：bg-surface-container-lowest rounded-lg p-6
文件名：font-body text-sm font-medium text-on-surface
文件大小：font-body text-xs text-on-surface-variant
进度条容器：mt-3 h-2 bg-surface-container-highest rounded-full
进度填充：h-2 bg-primary rounded-full transition-all duration-300
百分比：font-body text-sm font-medium text-primary mt-2
速度信息：font-body text-xs text-on-surface-variant mt-1
          "上传速度: 12.5 MB/s | 约 2 分钟"
```

### CompletedUploadItem

```
容器：flex items-center gap-3 py-3
图标：check_circle, text-green-500, size=20
文件名：font-body text-sm font-medium text-on-surface flex-1
文件大小：font-body text-xs text-on-surface-variant
链接："审核元数据" text-primary text-sm hover:underline
```

### EditorialTipCards

```
布局：grid grid-cols-2 gap-6 mt-8
卡片 1 (渐变)：
  bg-gradient-to-br from-primary to-primary-container
  rounded-lg p-6 text-white
  标题：font-headline text-lg font-bold
  内容：font-body text-sm text-white/80
卡片 2 (tertiary)：
  bg-tertiary-fixed rounded-lg p-6
  标题：font-headline text-lg font-bold text-on-tertiary-fixed
  内容：font-body text-sm text-on-tertiary-fixed-variant
```


## H. HTML 原型参考代码

> 此 HTML 原型使用 CDN Tailwind CSS 构建，包含页面的完整 DOM 结构和精确的 Tailwind class。开发时请参考其中的布局结构、颜色 token、间距和组件样式。

> 源文件：`ui/stitch_grace_video_management/grace_video_upload/code.html`

```html
<!DOCTYPE html>

<html class="light" lang="zh-CN"><head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<script src="https://cdn.tailwindcss.com?plugins=forms,container-queries"></script>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&amp;family=Manrope:wght@700;800&amp;family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
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
                    borderRadius: { "DEFAULT": "0.25rem", "lg": "0.5rem", "xl": "0.75rem", "full": "9999px" },
                },
            },
        }
    </script>
<style>
        .material-symbols-outlined {
            font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
            display: inline-block;
            line-height: 1;
            text-transform: none;
            letter-spacing: normal;
            word-wrap: normal;
            white-space: nowrap;
            direction: ltr;
        }
        .upload-dashed-border {
            background-image: url("data:image/svg+xml,%3csvg width='100%25' height='100%25' xmlns='http://www.w3.org/2000/svg'%3e%3crect width='100%25' height='100%25' fill='none' rx='12' ry='12' stroke='%23006EF2FF' stroke-width='2' stroke-dasharray='8%2c 12' stroke-dashoffset='0' stroke-linecap='square'/%3e%3c/svg%3e");
            border-radius: 12px;
        }
    </style>
</head>
<body class="bg-surface font-body text-on-surface antialiased">
<!-- SideNavBar Shell -->
<aside class="h-screen w-[240px] fixed left-0 top-0 bg-[#001529] flex flex-col justify-between pb-8 z-20 shadow-[0_8px_32px_rgba(0,26,67,0.06)]">
<div>
<div class="py-8 px-6 mb-4">
<div class="text-white text-2xl font-bold flex items-center gap-2 font-headline">
<span class="material-symbols-outlined text-blue-400" style="font-variation-settings: 'FILL' 1;">restaurant_menu</span>
                    Grace
                </div>
<div class="text-slate-400 text-xs mt-1 font-inter">Video Distribution</div>
</div>
<nav class="flex flex-col gap-1">
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors hover:bg-white/5 flex items-center gap-3" href="#">
<span class="material-symbols-outlined">dashboard</span>
                    仪表盘
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors hover:bg-white/5 flex items-center gap-3" href="#">
<span class="material-symbols-outlined">video_library</span>
                    视频管理
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors hover:bg-white/5 flex items-center gap-3" href="#">
<span class="material-symbols-outlined">fact_check</span>
                    元数据审核
                </a>
<!-- Active State: 视频发布 (Uploading state) -->
<a class="bg-blue-600/10 text-blue-400 border-r-4 border-blue-500 py-4 px-6 flex items-center gap-3" href="#">
<span class="material-symbols-outlined" style="font-variation-settings: 'FILL' 1;">publish</span>
                    视频发布
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors hover:bg-white/5 flex items-center gap-3" href="#">
<span class="material-symbols-outlined">alt_route</span>
                    推广渠道
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors hover:bg-white/5 flex items-center gap-3" href="#">
<span class="material-symbols-outlined">task</span>
                    推广任务
                </a>
</nav>
</div>
<div class="px-6">
<a class="text-slate-400 hover:text-white py-4 flex items-center gap-3 transition-colors" href="#">
<span class="material-symbols-outlined">settings</span>
                设置
            </a>
</div>
</aside>
<!-- TopNavBar Shell -->
<header class="h-16 fixed top-0 right-0 left-[240px] w-[calc(100%-240px)] z-10 bg-white/80 backdrop-blur-md flex items-center justify-between px-8">
<div class="flex items-center gap-2">
<span class="text-slate-500 font-inter text-sm">发布中心</span>
<span class="material-symbols-outlined text-slate-400 text-sm">chevron_right</span>
<span class="text-blue-600 font-semibold font-inter text-sm">上传视频</span>
</div>
<div class="flex items-center gap-6">
<button class="relative text-slate-500 hover:bg-slate-50 p-2 rounded-lg transition-all duration-200">
<span class="material-symbols-outlined">notifications</span>
<span class="absolute top-2 right-2 w-2 h-2 bg-error rounded-full border-2 border-white"></span>
</button>
<div class="flex items-center gap-3 cursor-pointer hover:bg-slate-50 p-1 rounded-lg transition-all">
<img alt="User Avatar" class="w-8 h-8 rounded-full bg-secondary-container object-cover" data-alt="close-up portrait of a professional chef wearing a white toque with a friendly and welcoming expression" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBmkuJAwEae2_-k7KyFvecOoJYrnkyTVT41H_jc-q3c3jxMnLI64MEzJoivNjKefjme9kaP2RlO0_dZeNYXM6n63Z7kfxnr-XOsBi4gFqdCCSk8PwEvidhEiWAsVXFeH9h8DyPTUR_LSW8qrUjRI_m_-GcqTX9AeN4FzLUIQZeiJyK6U6OuUevx-Uzzse2hkOXHrsfrg0AIv3WzWGpa1hseOcg09cfpNZz23v2-V24c3k_C8KggBBttQwwJ9DX1U2FV9g_AakngQv-J"/>
<span class="text-sm font-medium text-on-surface">Grace Chef</span>
</div>
</div>
</header>
<!-- Main Content Canvas -->
<main class="pl-[240px] pt-16 min-h-screen">
<div class="max-w-5xl mx-auto px-12 py-10">
<!-- Hero Heading -->
<div class="mb-12">
<h1 class="font-headline text-[2.75rem] font-bold text-on-surface tracking-tight">发布您的佳作</h1>
<p class="text-slate-500 mt-2 font-body">将您的烹饪灵感分享给世界，支持多平台一键分发。</p>
</div>
<!-- Upload Area Section -->
<section class="bg-surface-container-lowest rounded-xl p-4 mb-8">
<div class="upload-dashed-border bg-[#F0F5FF]/50 p-16 flex flex-col items-center justify-center text-center">
<div class="w-20 h-20 bg-primary-fixed rounded-full flex items-center justify-center mb-6">
<span class="material-symbols-outlined text-primary text-4xl" style="font-variation-settings: 'FILL' 1;">cloud_upload</span>
</div>
<h2 class="text-xl font-headline font-bold mb-2">拖拽视频文件到此处，或点击选择文件</h2>
<p class="text-slate-500 mb-8 max-w-sm">支持 MP4、MOV、AVI、MKV，不超过 5GB。建议使用 1080p 或更高分辨率以获得最佳展示效果。</p>
<button class="px-8 py-3 rounded-lg border-2 border-primary text-primary font-semibold hover:bg-primary/5 transition-all flex items-center gap-2">
<span class="material-symbols-outlined">add_circle</span>
                        选择文件
                    </button>
</div>
</section>
<!-- Active Progress Card -->
<section class="mb-12">
<h3 class="font-headline text-lg font-bold mb-4 flex items-center gap-2">
<span class="w-2 h-2 bg-primary rounded-full"></span>
                    正在上传 (1)
                </h3>
<div class="bg-surface-container-lowest p-6 rounded-xl transition-all duration-300">
<div class="flex items-start gap-4">
<div class="w-12 h-12 bg-secondary-container rounded-lg flex items-center justify-center text-on-secondary-container">
<span class="material-symbols-outlined">movie</span>
</div>
<div class="flex-1">
<div class="flex justify-between items-start mb-2">
<div>
<h4 class="font-semibold text-on-surface">红烧肉制作教程.mp4</h4>
<p class="text-xs text-slate-500 mt-1">1.2 GB</p>
</div>
<button class="p-1 hover:bg-error-container hover:text-error rounded-md transition-colors">
<span class="material-symbols-outlined">close</span>
</button>
</div>
<!-- Progress Bar -->
<div class="relative h-2 w-full bg-surface-container-highest rounded-full overflow-hidden mt-4">
<div class="absolute top-0 left-0 h-full bg-primary rounded-full" style="width: 45%;"></div>
</div>
<div class="flex justify-between items-center mt-3">
<span class="text-xs text-slate-500 flex items-center gap-1">
<span class="material-symbols-outlined text-[14px]">speed</span>
                                    上传速度: 12.5 MB/s | 约 2 分钟
                                </span>
<span class="text-sm font-bold text-primary">45%</span>
</div>
</div>
</div>
</div>
</section>
<!-- Completed & Task List (Bento-ish Grid) -->
<section>
<h3 class="font-headline text-lg font-bold mb-4 flex items-center gap-2">
<span class="w-2 h-2 bg-green-500 rounded-full"></span>
                    已处理
                </h3>
<div class="grid grid-cols-1 gap-4">
<!-- Completed Item -->
<div class="bg-surface-container-lowest p-5 rounded-xl flex items-center gap-4 group hover:bg-surface-bright transition-all">
<div class="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center text-green-600">
<span class="material-symbols-outlined">check_circle</span>
</div>
<div class="flex-1 flex items-center justify-between">
<div>
<h4 class="font-medium text-sm text-on-surface">夏季清凉饮品特辑.mp4</h4>
<p class="text-xs text-green-600 mt-0.5 font-medium flex items-center gap-1">
                                    上传完成，正在生成元数据...
                                </p>
</div>
<div class="flex items-center gap-6">
<div class="w-32 h-1.5 bg-green-500 rounded-full"></div>
<button class="text-primary text-sm font-semibold hover:underline px-3 py-1 bg-primary/5 rounded">编辑元数据</button>
</div>
</div>
</div>
<!-- Another Recent Item -->
<div class="bg-surface-container-lowest p-5 rounded-xl flex items-center gap-4 group hover:bg-surface-bright transition-all opacity-80">
<div class="w-10 h-10 bg-slate-100 rounded-lg flex items-center justify-center text-slate-500">
<span class="material-symbols-outlined">description</span>
</div>
<div class="flex-1 flex items-center justify-between">
<div>
<h4 class="font-medium text-sm text-on-surface">家庭周末烘焙日.mov</h4>
<p class="text-xs text-slate-500 mt-0.5">已于 昨天 14:20 上传</p>
</div>
<div class="flex items-center gap-3">
<div class="flex -space-x-2">
<div class="w-6 h-6 rounded-full bg-red-500 flex items-center justify-center text-[10px] text-white ring-2 ring-white">YT</div>
<div class="w-6 h-6 rounded-full bg-blue-500 flex items-center justify-center text-[10px] text-white ring-2 ring-white">FB</div>
<div class="w-6 h-6 rounded-full bg-pink-500 flex items-center justify-center text-[10px] text-white ring-2 ring-white">IG</div>
</div>
<span class="text-xs text-slate-400">3 平台已发布</span>
</div>
</div>
</div>
</div>
</section>
<!-- Featured Tips (Editorial Component) -->
<section class="mt-16 grid grid-cols-3 gap-6">
<div class="col-span-2 bg-gradient-to-br from-primary to-primary-container p-8 rounded-2xl text-white relative overflow-hidden">
<div class="relative z-10">
<h4 class="font-headline text-xl mb-2">如何提升视频曝光率？</h4>
<p class="text-white/80 text-sm max-w-md mb-6 font-body">使用 Grace 的 AI 元数据增强工具，我们可以为您自动生成高流量标签和优化的视频描述。</p>
<button class="bg-white text-primary px-6 py-2 rounded-lg font-bold text-sm hover:bg-blue-50 transition-colors">立即开启 AI 增强</button>
</div>
<span class="material-symbols-outlined absolute -bottom-4 -right-4 text-[120px] opacity-10 rotate-12">auto_awesome</span>
</div>
<div class="bg-tertiary-fixed p-8 rounded-2xl flex flex-col justify-between">
<div>
<h4 class="font-headline text-lg text-on-tertiary-fixed mb-2">分发渠道统计</h4>
<p class="text-on-tertiary-fixed-variant text-xs">您已连接 8 个全球分发渠道</p>
</div>
<div class="flex justify-between items-end">
<span class="text-4xl font-headline font-extrabold text-on-tertiary-fixed tracking-tight">85%</span>
<span class="text-[10px] font-bold px-2 py-1 bg-on-tertiary-fixed text-white rounded-full">同步率极高</span>
</div>
</div>
</section>
</div>
</main>
</body></html>
```
