# 页面：分发与推广向导 (Distribution & Promotion)

> 依赖文档：[02-design-system.md](./02-design-system.md)、[03-shared-infrastructure.md](./03-shared-infrastructure.md)
> 设计稿：`ui/stitch_grace_video_management/grace_distribution_promotion/screen.png`
> HTML 原型：`ui/stitch_grace_video_management/grace_distribution_promotion/code.html`
> 路由：`/videos/:videoId/distribute`

## A. 页面概览

分发与推广向导是一个 3 步流程页面：Step 1 确认视频信息 → Step 2 选择分发平台 → Step 3 配置推广文案。用户完成后可一键发布视频到平台并执行推广任务。

## B. 组件层级树

```
DistributionPromotionPage
├── PageHeader ("分发与推广" 标题)
├── StepWizard
│   ├── StepIndicator
│   │   ├── StepDot (1 - 确认信息) ← completed/active/future
│   │   ├── StepLine
│   │   ├── StepDot (2 - 选择平台)
│   │   ├── StepLine
│   │   └── StepDot (3 - 推广配置)
│   └── StepContent
│       ├── [Step 1] VideoConfirmation
│       │   ├── VideoPreviewCard (缩略图 + 视频信息)
│       │   ├── MetadataSummaryCard (标题 + 描述 + 标签)
│       │   └── NextButton
│       ├── [Step 2] PlatformSelection
│       │   ├── PlatformCardGrid (grid-cols-4)
│       │   │   ├── PlatformCard (YouTube - 可选, active)
│       │   │   ├── PlatformCard (抖音 - 禁用, coming soon)
│       │   │   ├── PlatformCard (B站 - 禁用, coming soon)
│       │   │   └── PlatformCard (小红书 - 禁用, coming soon)
│       │   ├── PrivacyStatusSelect (public/unlisted/private)
│       │   └── NavigationButtons (Back + Next)
│       └── [Step 3] PromotionConfig
│           ├── PromotionCopyGrid (grid-cols-3)
│           │   └── PromotionCopyCard × N
│           │       ├── ChannelHeader (图标 + 渠道名)
│           │       ├── MethodBadge (POST/THREAD/NOTE)
│           │       ├── TitleInput (可编辑推广标题)
│           │       ├── BodyTextarea (可编辑推广正文)
│           │       └── RegenerateButton
│           └── ActionFooter
│               ├── BackButton
│               └── ConfirmPublishButton (渐变 CTA + rocket_launch)
```

## C. API 端点

| 端点 | 方法 | Hook | 调用时机 |
|------|------|------|----------|
| B6. `/api/videos/{videoId}` | GET | `useVideoDetail(videoId)` | Step 1 加载视频 + 元数据 |
| D5. `/api/distribution/platforms` | GET | `usePlatforms()` | Step 2 加载平台列表 |
| D1. `/api/distribution/publish` | POST | `usePublish()` | 确认发布时 |
| D2. `/api/distribution/status/{taskId}` | GET | `usePublishStatus(taskId)` | 发布后轮询状态 |
| F1. `/api/promotions/generate-copy` | POST | `useGenerateCopy()` | Step 3 进入时自动生成 |
| F2. `/api/promotions/execute` | POST | `useExecutePromotion()` | 确认发布时同时执行推广 |

### 发布 + 推广流程

```
1. Step 1: 加载视频详情 (B6)，展示确认信息
2. Step 2: 加载平台列表 (D5)，用户选择平台
3. Step 3: 调用生成推广文案 (F1)，用户可编辑
4. 点击"确认发布"：
   a. 调用 D1 发布视频到选定平台
   b. 收到 taskId 后开始轮询 D2 跟踪发布进度
   c. 同时调用 F2 执行推广任务
   d. 全部完成后显示结果摘要
```

## D. 状态管理

| 类型 | 内容 | 管理方式 |
|------|------|----------|
| 服务端状态 | 视频详情 | React Query `useVideoDetail` |
| 服务端状态 | 平台列表 | React Query `usePlatforms` |
| 服务端状态 | 推广文案 | React Query mutation 结果 |
| 本地状态 | 当前步骤 (1/2/3) | `useState<number>(1)` |
| 本地状态 | 选中的平台 | `useState<string>('youtube')` |
| 本地状态 | 隐私状态 | `useState<string>('public')` |
| 本地状态 | 编辑中的推广文案 | `useState<EditablePromotionCopy[]>` |
| 本地状态 | 发布中/结果状态 | `useState` |

## E. 关键交互

| 用户操作 | 触发行为 | 状态变更 | UI 反馈 |
|---------|---------|---------|---------|
| 点击 Next (Step 1→2) | 校验视频状态 | step = 2 | Step 指示器更新 |
| 选择平台卡片 | 更新选中平台 | selectedPlatform 更新 | 卡片高亮 (border-2 border-primary + check_circle) |
| 点击禁用平台 | 无操作 | - | 显示"即将支持"提示 |
| 点击 Next (Step 2→3) | 触发 F1 生成推广文案 | step = 3, loading | 文案卡片 skeleton → 内容填充 |
| 编辑推广标题/正文 | 更新本地文案数据 | editableCopies 更新 | 输入即时反映 |
| 点击单卡片"重新生成" | 调用 F1 仅该渠道 | 该卡片 loading | 单卡片刷新 |
| 点击"确认发布" | 调用 D1 + F2 | publishing = true | 加载状态 → 结果摘要 |
| 发布成功 | 跳转或显示结果 | - | Toast "发布成功" + 结果链接 |
| 点击 Back | 回退步骤 | step -= 1 | Step 指示器更新 |

## F. 错误处理

| 场景 | 错误码 | 用户提示 | 恢复操作 |
|------|--------|---------|---------|
| 视频不存在 | 1008 | "视频不存在" | 返回视频列表 |
| 视频未就绪 | 3005 | "请先确认元数据" | 跳转元数据审核页 |
| 平台未授权 | 3003 | "请先连接 YouTube 账户" | 跳转设置页 |
| 授权过期 | 3002 | "授权已过期，请重新连接" | 跳转设置页 |
| AI 文案生成失败 | 9001 | "AI 服务暂时不可用" | 允许手动编写文案 |
| 发布失败 | 3007 | 显示具体错误信息 | 提供重试按钮 |
| 推广部分失败 | 4003 | 显示各渠道结果 | 失败渠道提供重试 |

## G. 视觉实现备注

### StepIndicator

```
容器：flex items-center justify-center mb-12
步骤圆点：
  已完成：w-10 h-10 rounded-full bg-primary flex items-center justify-center
          check 图标 text-white
  当前：  w-10 h-10 rounded-full bg-primary ring-4 ring-primary-fixed
          flex items-center justify-center text-white font-bold
  未来：  w-10 h-10 rounded-full bg-surface-container-high
          flex items-center justify-center text-on-surface-variant font-medium
连接线：
  已完成：h-0.5 w-24 bg-primary
  未来：  h-0.5 w-24 bg-surface-container-high
步骤标签：mt-2 text-xs font-medium
  当前：text-primary
  其他：text-on-surface-variant
```

### PlatformCard

```
可用 + 选中：
  容器：border-2 border-primary rounded-xl p-6 cursor-pointer relative
        bg-surface-container-lowest
  右上角：absolute top-3 right-3, check_circle 图标 text-primary
  Logo：w-12 h-12 居中
  名称：font-body text-sm font-medium text-center mt-3

可用 + 未选中：
  容器：border-2 border-transparent rounded-xl p-6 cursor-pointer
        bg-surface-container-lowest hover:bg-surface-bright

禁用 (coming soon)：
  容器：rounded-xl p-6 opacity-60 grayscale cursor-not-allowed
        bg-surface-container-lowest
  标签：mt-2 text-xs text-on-surface-variant "即将支持"
```

### PromotionCopyCard

```
容器：bg-surface-container-lowest rounded-lg p-5
渠道头部：flex items-center gap-3 mb-4
  图标容器：w-10 h-10 rounded-full bg-{channel-color} flex items-center justify-center
  渠道名：font-body text-sm font-medium
  方式 badge：ml-auto, bg-surface-container-high text-on-surface-variant
              px-2 py-0.5 rounded text-xs font-medium
              "POST" / "THREAD" / "NOTE"
标题输入：
  bg-surface-container-low rounded-md px-3 py-2 text-sm w-full
正文文本域：
  bg-surface-container-low rounded-md px-3 py-2 text-sm w-full h-32 resize-none mt-3
重新生成按钮：
  mt-3 flex items-center gap-1 text-xs text-primary hover:underline cursor-pointer
  refresh 图标 size=14 + "重新生成"
```

### ConfirmPublishButton

```
bg-gradient-to-r from-primary to-primary-container
text-white rounded-lg px-8 py-3 font-medium text-sm
flex items-center gap-2
rocket_launch 图标 + "确认发布"
hover: shadow-lg shadow-primary/20
disabled: opacity-50 cursor-not-allowed
```

### 页面布局

```
Step 1: max-w-2xl mx-auto
Step 2: max-w-4xl mx-auto, platform cards grid-cols-4
Step 3: full width, promotion cards grid-cols-3 gap-6
Footer: flex items-center justify-between mt-8
```


## H. HTML 原型参考代码

> 此 HTML 原型使用 CDN Tailwind CSS 构建，包含页面的完整 DOM 结构和精确的 Tailwind class。开发时请参考其中的布局结构、颜色 token、间距和组件样式。

> 源文件：`ui/stitch_grace_video_management/grace_distribution_promotion/code.html`

```html
<!DOCTYPE html>

<html class="light" lang="zh-CN"><head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<script src="https://cdn.tailwindcss.com?plugins=forms,container-queries"></script>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&amp;family=Manrope:wght@600;700;800&amp;family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
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
      body {
        font-family: 'Inter', sans-serif;
        background-color: #f9f9f9;
      }
      .font-headline { font-family: 'Manrope', sans-serif; }
    </style>
</head>
<body class="bg-background text-on-background">
<!-- SideNavBar -->
<aside class="h-screen w-[240px] fixed left-0 top-0 bg-[#001529] flex flex-col justify-between pb-8 shadow-[0_8px_32px_rgba(0,26,67,0.06)] z-20">
<div class="flex flex-col">
<div class="p-6 mb-8">
<div class="text-white text-2xl font-bold flex items-center gap-2 font-headline">
<span class="material-symbols-outlined text-blue-400" data-icon="restaurant">restaurant</span>
                    Grace
                </div>
<div class="text-slate-400 text-xs mt-1 font-inter">Video Distribution</div>
</div>
<nav class="flex flex-col space-y-1">
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-3 hover:bg-white/5" href="#">
<span class="material-symbols-outlined text-xl" data-icon="dashboard">dashboard</span>
<span class="text-sm font-inter">仪表盘</span>
</a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-3 hover:bg-white/5" href="#">
<span class="material-symbols-outlined text-xl" data-icon="video_library">video_library</span>
<span class="text-sm font-inter">视频管理</span>
</a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-3 hover:bg-white/5" href="#">
<span class="material-symbols-outlined text-xl" data-icon="fact_check">fact_check</span>
<span class="text-sm font-inter">元数据审核</span>
</a>
<a class="bg-blue-600/10 text-blue-400 border-r-4 border-blue-500 py-4 px-6 flex items-center gap-3" href="#">
<span class="material-symbols-outlined text-xl" data-icon="publish">publish</span>
<span class="text-sm font-inter">视频发布</span>
</a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-3 hover:bg-white/5" href="#">
<span class="material-symbols-outlined text-xl" data-icon="alt_route">alt_route</span>
<span class="text-sm font-inter">推广渠道</span>
</a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-3 hover:bg-white/5" href="#">
<span class="material-symbols-outlined text-xl" data-icon="task">task</span>
<span class="text-sm font-inter">推广任务</span>
</a>
</nav>
</div>
<div class="px-2">
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-3 hover:bg-white/5 rounded-lg" href="#">
<span class="material-symbols-outlined text-xl" data-icon="settings">settings</span>
<span class="text-sm font-inter">设置</span>
</a>
</div>
</aside>
<!-- TopNavBar -->
<header class="h-16 fixed top-0 right-0 left-[240px] w-[calc(100%-240px)] z-10 bg-white/80 backdrop-blur-md flex items-center justify-between px-8">
<div class="flex items-center gap-2">
<span class="text-slate-400 text-sm font-inter">视频分发</span>
<span class="text-slate-300">/</span>
<span class="text-blue-600 font-semibold text-sm font-inter">发布配置</span>
</div>
<div class="flex items-center gap-6">
<button class="relative text-slate-500 hover:bg-slate-50 p-2 rounded-lg transition-all">
<span class="material-symbols-outlined" data-icon="notifications">notifications</span>
<span class="absolute top-2 right-2 w-2 h-2 bg-error rounded-full"></span>
</button>
<div class="flex items-center gap-3">
<div class="text-right">
<div class="text-sm font-semibold text-on-surface">Grace Admin</div>
<div class="text-[10px] text-slate-400 uppercase tracking-wider">Premium Plan</div>
</div>
<img alt="User Avatar" class="w-8 h-8 rounded-full object-cover ring-2 ring-primary/10" data-alt="Close-up portrait of a professional woman with a friendly expression in a brightly lit modern office setting" src="https://lh3.googleusercontent.com/aida-public/AB6AXuCQvns_BtoKWCaDjpfPL2l8o8u7d43JGFvLYm4HqgjDjtRv2J5AcmpnYXaqVSo3D0vOOZWn93d_TnoyZ_4g1AjNwnsxrhFGpeSEQ8-qv-I0J22-zL6CC9mvunGxgFpdD3AqD8S2qiqotarjD-Lz4A3Bl4-5AtOViTiEQTYiS6YglI23H0YoAJ5wmJMnhWveEFLi5BUd08CHPf-ZE-uSkFDLx-AXEymTewiH7FbtP8RPl_xwdOqwL9dzBJqZfjCfH3qjdP31BRXE87EF"/>
</div>
</div>
</header>
<!-- Main Content -->
<main class="ml-[240px] pt-24 pb-12 px-12 min-h-screen">
<!-- Step Indicator -->
<div class="max-w-4xl mx-auto mb-16">
<div class="flex items-center justify-between relative">
<!-- Progress Line -->
<div class="absolute top-1/2 left-0 w-full h-0.5 bg-surface-container-high -translate-y-1/2 -z-10"></div>
<div class="absolute top-1/2 left-0 w-1/2 h-0.5 bg-primary -translate-y-1/2 -z-10 transition-all duration-500"></div>
<!-- Step 1 -->
<div class="flex flex-col items-center gap-2 bg-background px-4">
<div class="w-10 h-10 rounded-full bg-primary text-white flex items-center justify-center font-bold shadow-lg shadow-primary/20">
<span class="material-symbols-outlined" data-icon="check">check</span>
</div>
<span class="text-sm font-medium text-on-surface">选择视频</span>
</div>
<!-- Step 2 (Active) -->
<div class="flex flex-col items-center gap-2 bg-background px-4">
<div class="w-10 h-10 rounded-full bg-primary text-white flex items-center justify-center font-bold ring-4 ring-primary-fixed shadow-lg shadow-primary/20">
                        2
                    </div>
<span class="text-sm font-bold text-primary">选择平台</span>
</div>
<!-- Step 3 -->
<div class="flex flex-col items-center gap-2 bg-background px-4">
<div class="w-10 h-10 rounded-full bg-surface-container-high text-slate-400 flex items-center justify-center font-bold">
                        3
                    </div>
<span class="text-sm font-medium text-slate-400">确认发布</span>
</div>
</div>
</div>
<div class="max-w-6xl mx-auto space-y-12">
<!-- Platform Selection Grid -->
<section>
<h2 class="font-headline text-2xl font-bold mb-6 text-on-surface">选择发布平台</h2>
<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
<!-- YouTube Card (Active) -->
<div class="group relative bg-surface-container-lowest p-6 rounded-xl border-2 border-primary shadow-[0_8px_32px_rgba(0,87,194,0.04)] cursor-pointer transition-all hover:scale-[1.02]">
<div class="absolute top-4 right-4 text-primary">
<span class="material-symbols-outlined" data-icon="check_circle" style="font-variation-settings: 'FILL' 1;">check_circle</span>
</div>
<div class="w-14 h-14 bg-red-50 rounded-lg flex items-center justify-center mb-4">
<span class="material-symbols-outlined text-red-600 text-3xl" data-icon="video_library">video_library</span>
</div>
<h3 class="font-bold text-lg mb-1">YouTube</h3>
<p class="text-xs text-green-600 flex items-center gap-1">
<span class="w-1.5 h-1.5 bg-green-500 rounded-full"></span>
                            OAuth 已连接
                        </p>
</div>
<!-- Douyin (Disabled) -->
<div class="bg-surface-container-low p-6 rounded-xl border-2 border-transparent opacity-60 grayscale cursor-not-allowed">
<div class="w-14 h-14 bg-slate-200 rounded-lg flex items-center justify-center mb-4">
<span class="material-symbols-outlined text-slate-600 text-3xl" data-icon="music_video">music_video</span>
</div>
<h3 class="font-bold text-lg mb-1 text-slate-500">抖音</h3>
<p class="text-xs text-slate-400">即将支持</p>
</div>
<!-- Bilibili (Disabled) -->
<div class="bg-surface-container-low p-6 rounded-xl border-2 border-transparent opacity-60 grayscale cursor-not-allowed">
<div class="w-14 h-14 bg-slate-200 rounded-lg flex items-center justify-center mb-4">
<span class="material-symbols-outlined text-slate-600 text-3xl" data-icon="smart_display">smart_display</span>
</div>
<h3 class="font-bold text-lg mb-1 text-slate-500">B站</h3>
<p class="text-xs text-slate-400">即将支持</p>
</div>
<!-- Little Red Book (Disabled) -->
<div class="bg-surface-container-low p-6 rounded-xl border-2 border-transparent opacity-60 grayscale cursor-not-allowed">
<div class="w-14 h-14 bg-slate-200 rounded-lg flex items-center justify-center mb-4">
<span class="material-symbols-outlined text-slate-600 text-3xl" data-icon="photo_library">photo_library</span>
</div>
<h3 class="font-bold text-lg mb-1 text-slate-500">小红书</h3>
<p class="text-xs text-slate-400">即将支持</p>
</div>
</div>
</section>
<!-- Promotion Social Copy -->
<section>
<div class="flex items-center justify-between mb-6">
<h2 class="font-headline text-2xl font-bold text-on-surface">AI 生成推广文案</h2>
<span class="text-xs font-medium text-primary bg-primary/10 px-3 py-1 rounded-full flex items-center gap-1">
<span class="material-symbols-outlined text-sm" data-icon="auto_awesome">auto_awesome</span>
                        智能优化中
                    </span>
</div>
<div class="grid grid-cols-1 md:grid-cols-3 gap-6">
<!-- Weibo Card -->
<div class="bg-surface-container-lowest p-6 rounded-xl shadow-[0_8px_32px_rgba(0,26,67,0.04)] flex flex-col gap-4">
<div class="flex items-center justify-between">
<div class="flex items-center gap-2">
<span class="material-symbols-outlined text-red-500" data-icon="campaign">campaign</span>
<span class="font-bold text-sm">Weibo</span>
</div>
<span class="text-[10px] font-bold text-slate-400 bg-slate-100 px-2 py-0.5 rounded">POST</span>
</div>
<div>
<label class="text-[10px] text-slate-400 font-bold mb-1 block uppercase tracking-wider">标题</label>
<input class="w-full bg-surface-container-low border-none rounded-lg text-sm font-medium focus:ring-2 focus:ring-primary/40" type="text" value="这道秘制红烧肉，好吃到舔盘！🍲"/>
</div>
<div>
<label class="text-[10px] text-slate-400 font-bold mb-1 block uppercase tracking-wider">正文</label>
<textarea class="w-full bg-surface-container-low border-none rounded-lg text-sm leading-relaxed h-32 focus:ring-2 focus:ring-primary/40">在这里分享我的私藏菜谱！今天教大家做超级入味的红烧肉，入口即化，肥而不腻。点击链接看完整教程：https://grace.app/v/cooking-101 #美食分享 #红烧肉教程</textarea>
</div>
<div class="mt-auto flex justify-end">
<button class="text-primary text-xs font-bold flex items-center gap-1 hover:underline">
<span class="material-symbols-outlined text-sm" data-icon="refresh">refresh</span>
                                重新生成
                            </button>
</div>
</div>
<!-- Reddit Card -->
<div class="bg-surface-container-lowest p-6 rounded-xl shadow-[0_8px_32px_rgba(0,26,67,0.04)] flex flex-col gap-4">
<div class="flex items-center justify-between">
<div class="flex items-center gap-2">
<span class="material-symbols-outlined text-orange-600" data-icon="forum">forum</span>
<span class="font-bold text-sm">Reddit</span>
</div>
<span class="text-[10px] font-bold text-slate-400 bg-slate-100 px-2 py-0.5 rounded">THREAD</span>
</div>
<div>
<label class="text-[10px] text-slate-400 font-bold mb-1 block uppercase tracking-wider">标题</label>
<input class="w-full bg-surface-container-low border-none rounded-lg text-sm font-medium focus:ring-2 focus:ring-primary/40" type="text" value="Secrets to the perfect Braised Pork Belly (Dongpo Rou)"/>
</div>
<div>
<label class="text-[10px] text-slate-400 font-bold mb-1 block uppercase tracking-wider">正文</label>
<textarea class="w-full bg-surface-container-low border-none rounded-lg text-sm leading-relaxed h-32 focus:ring-2 focus:ring-primary/40">Hey r/Cooking! Just wanted to share my technique for traditional braised pork. The secret is all in the slow simmer with rock sugar and dark soy sauce. Full breakdown here: https://grace.app/v/pork-belly</textarea>
</div>
<div class="mt-auto flex justify-end">
<button class="text-primary text-xs font-bold flex items-center gap-1 hover:underline">
<span class="material-symbols-outlined text-sm" data-icon="refresh">refresh</span>
                                重新生成
                            </button>
</div>
</div>
<!-- Douban Card -->
<div class="bg-surface-container-lowest p-6 rounded-xl shadow-[0_8px_32px_rgba(0,26,67,0.04)] flex flex-col gap-4">
<div class="flex items-center justify-between">
<div class="flex items-center gap-2">
<span class="material-symbols-outlined text-green-700" data-icon="book">book</span>
<span class="font-bold text-sm">Douban</span>
</div>
<span class="text-[10px] font-bold text-slate-400 bg-slate-100 px-2 py-0.5 rounded">NOTE</span>
</div>
<div>
<label class="text-[10px] text-slate-400 font-bold mb-1 block uppercase tracking-wider">标题</label>
<input class="w-full bg-surface-container-low border-none rounded-lg text-sm font-medium focus:ring-2 focus:ring-primary/40" type="text" value="关于冬日里那一碗红烧肉的治愈时刻"/>
</div>
<div>
<label class="text-[10px] text-slate-400 font-bold mb-1 block uppercase tracking-wider">正文</label>
<textarea class="w-full bg-surface-container-low border-none rounded-lg text-sm leading-relaxed h-32 focus:ring-2 focus:ring-primary/40">美食不仅仅是味觉的享受，更是一种情感的寄托。今天的视频里，我记录了这道红烧肉背后的故事。详细制作过程：https://grace.app/v/story-food</textarea>
</div>
<div class="mt-auto flex justify-end">
<button class="text-primary text-xs font-bold flex items-center gap-1 hover:underline">
<span class="material-symbols-outlined text-sm" data-icon="refresh">refresh</span>
                                重新生成
                            </button>
</div>
</div>
</div>
</section>
<!-- Bottom Action Bar -->
<footer class="flex items-center justify-between pt-8 border-t border-slate-100">
<button class="px-8 py-3 rounded-lg text-slate-500 font-bold flex items-center gap-2 hover:bg-slate-100 transition-all">
<span class="material-symbols-outlined" data-icon="arrow_back">arrow_back</span>
                    返回
                </button>
<button class="px-12 py-3.5 rounded-lg bg-gradient-to-r from-primary to-primary-container text-white font-bold flex items-center gap-3 shadow-lg shadow-primary/30 hover:scale-105 active:scale-95 transition-all">
                    确认发布
                    <span class="material-symbols-outlined" data-icon="rocket_launch">rocket_launch</span>
</button>
</footer>
</div>
</main>
</body></html>
```
