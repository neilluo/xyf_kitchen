# 页面：元数据审核 (Metadata Review)

> 依赖文档：[02-design-system.md](./02-design-system.md)、[03-shared-infrastructure.md](./03-shared-infrastructure.md)
> 设计稿：`ui/stitch_grace_video_management/grace_metadata_review/screen.png`
> HTML 原型：`ui/stitch_grace_video_management/grace_metadata_review/code.html`
> 路由：`/videos/:videoId/metadata`

## A. 页面概览

元数据审核页面是双栏布局。左侧展示视频预览和基本信息，右侧为 AI 生成的元数据编辑器（标题、描述、标签）。用户可编辑、重新生成、保存草稿或确认元数据。确认后视频状态变为 `READY_TO_PUBLISH`，不可逆。

## B. 组件层级树

```
MetadataReviewPage
├── PageHeader ("元数据审核" 标题 + AiBadge)
└── ContentGrid (lg:grid-cols-2 gap-8)
    ├── LeftColumn
    │   ├── VideoPreviewCard
    │   │   ├── VideoPlayer (aspect-video, 带播放按钮覆层)
    │   │   │   ├── Thumbnail / Video
    │   │   │   ├── PlayButton (居中, 半透明黑底)
    │   │   │   └── ProgressBar (底部, h-1)
    │   │   └── VideoInfoGrid (2×2 网格)
    │   │       ├── InfoItem ("文件名", fileName)
    │   │       ├── InfoItem ("格式", format)
    │   │       ├── InfoItem ("大小", fileSize)
    │   │       └── InfoItem ("时长", duration)
    │   └── StatusInfoCard (可选, 显示当前视频状态流转)
    └── RightColumn
        └── MetadataEditorCard
            ├── EditorHeader ("AI 生成的元数据" + AiBadge)
            ├── TitleInput
            │   ├── Label ("标题")
            │   ├── Input (带字符计数器 42/100)
            │   └── CharCounter
            ├── DescriptionTextarea
            │   ├── Label ("描述")
            │   ├── Textarea (带字符计数器 350/5000)
            │   └── CharCounter
            ├── TagsSection
            │   ├── Label ("标签")
            │   ├── TagChipList
            │   │   └── TagChip × N (可删除)
            │   └── TagInput (添加新标签)
            └── ActionFooter
                ├── Left: RegenerateButton ("重新生成")
                └── Right: SaveDraftButton + ConfirmButton (渐变 CTA)
```

## C. API 端点

| 端点 | 方法 | Hook | 调用时机 |
|------|------|------|----------|
| B6. `/api/videos/{videoId}` | GET | `useVideoDetail(videoId)` | 页面加载，获取视频信息 + 已有元数据 |
| C5. `/api/metadata/video/{videoId}` | GET | `useVideoMetadata(videoId)` | 刷新元数据（重新生成后） |
| C1. `/api/metadata/generate` | POST | `useGenerateMetadata()` | 如无元数据，手动触发生成 |
| C2. `/api/metadata/{id}` | PUT | `useUpdateMetadata()` | 保存草稿 |
| C3. `/api/metadata/{id}/regenerate` | POST | `useRegenerateMetadata()` | 点击"重新生成" |
| C4. `/api/metadata/{id}/confirm` | POST | `useConfirmMetadata()` | 点击"确认元数据" |

### 数据加载流程

```
1. 进入页面，从 URL 获取 videoId
2. 调用 B6 获取视频详情（含 metadata 字段）
3. 如果 metadata 为 null：
   - 显示"正在生成元数据..."状态
   - 视频状态为 UPLOADED 时，后端会自动通过事件触发生成
   - 可提供手动"生成元数据"按钮（调用 C1）
4. 如果 metadata 存在：
   - 填充编辑器表单
   - 如果已确认（状态 READY_TO_PUBLISH+），编辑器变为只读
```

## D. 状态管理

| 类型 | 内容 | 管理方式 |
|------|------|----------|
| 服务端状态 | 视频详情 + 元数据 | React Query `useVideoDetail` |
| 本地状态 | 编辑中的表单数据 (title, description, tags) | `useState` |
| 本地状态 | 表单是否有未保存修改 (isDirty) | `useState<boolean>` |
| 本地状态 | 新标签输入值 | `useState<string>` |

## E. 关键交互

| 用户操作 | 触发行为 | 状态变更 | UI 反馈 |
|---------|---------|---------|---------|
| 编辑标题 | 更新本地 title | isDirty = true | 字符计数器更新 |
| 编辑描述 | 更新本地 description | isDirty = true | 字符计数器更新 |
| 删除标签 | 从 tags 数组移除 | isDirty = true | 标签消失动画 |
| 添加标签 | 在输入框回车或点击添加 | tags 新增，isDirty = true | 新标签出现 |
| 点击"重新生成" | 调用 C3 | loading 状态 | 编辑器内容刷新 + AI 标记 |
| 点击"保存草稿" | 调用 C2 | isDirty = false | Toast "保存成功" |
| 点击"确认元数据" | 确认对话框 → 调用 C4 | 视频状态变为 READY_TO_PUBLISH | Toast "元数据已确认" + 编辑器变只读 |
| 已确认状态下 | 编辑器所有字段只读 | - | 显示"元数据已确认"提示 |

## F. 错误处理

| 场景 | 错误码 | 用户提示 | 恢复操作 |
|------|--------|---------|---------|
| 视频不存在 | 1008 | "视频不存在" | 返回视频列表 |
| 元数据校验失败 | 2001 | 显示具体字段错误 | 高亮错误字段 |
| 元数据已确认 | 2003 | "元数据已确认，无法编辑" | 编辑器只读 |
| AI 服务不可用 | 9001 | "AI 服务暂时不可用" | 允许手动填写 |
| 标题超长 | - | 字符计数变红 (>100) | 客户端校验阻止提交 |
| 描述超长 | - | 字符计数变红 (>5000) | 客户端校验阻止提交 |
| 标签过少 | - | "至少需要 5 个标签" | 客户端校验阻止确认 |

## G. 视觉实现备注

### 整体布局

```
容器：grid lg:grid-cols-2 gap-8
响应式：小屏幕下 grid-cols-1，左右变上下
```

### VideoPreviewCard

```
容器：bg-surface-container-lowest rounded-lg overflow-hidden
视频预览：
  容器：aspect-video relative bg-on-surface rounded-t-lg overflow-hidden
  缩略图：w-full h-full object-cover
  播放按钮覆层：absolute inset-0 flex items-center justify-center
    按钮：w-16 h-16 bg-black/50 rounded-full flex items-center justify-center
    图标：play_arrow, text-white, size=32
  进度条（底部）：absolute bottom-0 left-0 right-0 h-1 bg-white/30
    填充：h-1 bg-primary
信息网格：
  容器：p-6 grid grid-cols-2 gap-4
  单项：
    标签：font-label text-xs text-on-surface-variant uppercase
    值：font-body text-sm font-medium text-on-surface mt-1
```

### MetadataEditorCard

```
容器：bg-surface-container-lowest rounded-lg p-6
      或使用毛玻璃效果：bg-white/80 backdrop-blur-[12px]

编辑器头部：flex items-center justify-between mb-6
  标题：font-headline text-lg font-bold text-on-surface
  AI Badge：bg-secondary-fixed text-on-secondary-fixed px-2 py-0.5 rounded-full
            text-xs font-medium flex items-center gap-1
            图标：auto_awesome, size=14

标题输入：
  标签：font-label text-xs font-medium text-on-surface-variant uppercase tracking-wider
  输入框：mt-2 w-full bg-surface-container-low rounded-md px-4 py-3
          text-sm font-body text-on-surface
          focus:ring-2 focus:ring-primary/40
  字符计数：text-right text-xs text-on-surface-variant mt-1
           超长时：text-error

描述文本域：
  textarea：h-40 resize-none, 其余同输入框

标签区域：
  标签列表：flex flex-wrap gap-2 mt-2
  单标签：bg-tertiary-fixed text-on-tertiary-fixed-variant
          px-3 py-1 rounded-full text-xs font-medium
          flex items-center gap-1
  关闭按钮：close 图标 size=14 cursor-pointer
            hover:bg-tertiary/20 rounded-full p-0.5
  输入框：inline-flex, bg-surface-container-low rounded-full
          px-3 py-1 text-xs, 回车添加

操作栏：
  容器：flex items-center justify-between mt-8 pt-6
       （顶部用 surface-container-high 分隔线或留白）
  左：重新生成按钮
    refresh 图标 + "重新生成"
    bg-surface-container-high text-on-surface rounded-lg px-4 py-2
  右：flex items-center gap-3
    保存草稿：bg-surface-container-high text-on-surface rounded-lg px-4 py-2
    确认元数据：bg-gradient-to-r from-primary to-primary-container text-white
               rounded-lg px-6 py-2.5 font-medium
               带 check_circle 图标
```

### AiBadge

```
容器：inline-flex items-center gap-1
      bg-secondary-fixed text-on-secondary-fixed
      px-2.5 py-1 rounded-full text-xs font-medium
图标：auto_awesome, size=14
文字："AI 生成"
```


## H. HTML 原型参考代码

> 此 HTML 原型使用 CDN Tailwind CSS 构建，包含页面的完整 DOM 结构和精确的 Tailwind class。开发时请参考其中的布局结构、颜色 token、间距和组件样式。

> 源文件：`ui/stitch_grace_video_management/grace_metadata_review/code.html`

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
            vertical-align: middle;
        }
        body { font-family: 'Inter', sans-serif; }
        h1, h2, h3 { font-family: 'Manrope', sans-serif; }
        .glass-panel {
            background: rgba(255, 255, 255, 0.8);
            backdrop-filter: blur(12px);
        }
    </style>
</head>
<body class="bg-surface text-on-surface flex min-h-screen">
<!-- SideNavBar -->
<aside class="h-screen w-[240px] fixed left-0 top-0 bg-[#001529] flex flex-col justify-between pb-8 z-20">
<div>
<div class="py-8 px-6 text-white text-2xl font-bold flex items-center gap-2 font-headline">
<span class="material-symbols-outlined text-blue-400" data-icon="restaurant">restaurant</span>
                Grace
            </div>
<nav class="mt-4">
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-3 hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined" data-icon="dashboard">dashboard</span>
                    仪表盘
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-3 hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined" data-icon="video_library">video_library</span>
                    视频管理
                </a>
<a class="bg-blue-600/10 text-blue-400 border-r-4 border-blue-500 py-4 px-6 flex items-center gap-3 font-body text-sm" href="#">
<span class="material-symbols-outlined" data-icon="fact_check">fact_check</span>
                    元数据审核
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-3 hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined" data-icon="publish">publish</span>
                    视频发布
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-3 hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined" data-icon="alt_route">alt_route</span>
                    推广渠道
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 transition-colors flex items-center gap-3 hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined" data-icon="task">task</span>
                    推广任务
                </a>
</nav>
</div>
<div class="px-6">
<a class="text-slate-400 hover:text-white py-4 transition-colors flex items-center gap-3 hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined" data-icon="settings">settings</span>
                设置
            </a>
</div>
</aside>
<!-- Main Content Area -->
<main class="flex-1 ml-[240px] flex flex-col min-h-screen">
<!-- TopNavBar -->
<header class="h-16 fixed top-0 right-0 left-[240px] w-[calc(100%-240px)] z-10 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md flex items-center justify-between px-8">
<div class="flex items-center gap-2">
<span class="text-slate-500 font-body text-sm">元数据审核</span>
<span class="text-slate-300">/</span>
<span class="text-blue-600 font-semibold font-body text-sm">编辑详情</span>
</div>
<div class="flex items-center gap-6">
<button class="text-on-surface-variant hover:bg-slate-50 p-2 rounded-lg transition-all">
<span class="material-symbols-outlined" data-icon="notifications">notifications</span>
</button>
<div class="w-8 h-8 rounded-full overflow-hidden bg-surface-container-high border-2 border-white shadow-sm">
<img alt="User Avatar" class="w-full h-full object-cover" data-alt="portrait of a professional chef smiling in a bright kitchen background" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBsOMlWmzd1a2vDFa-2YYrCMOHq0ggqoGbIuxJ5U2MuIRc7BWC65GW3Y_67247B1h4B_gMT6CBgZ2nobUn_BH4PTBh0dTE1YOTs1-TcCEZmC4Qn8zOy6P-4J_rT9EwElzXhUeZLPcx1dhfiFj7GHBS5MYUnWN6Okp8Ocm_mz6VTR2zgGR5e47x_Nm8ZBdjoNmg8Oucm__j_s1Var4sm5BCgkoO-1r0TLoATL2ClV_jLSPx-tNmedn1Yzs6fmt-Lp-42I0eb7BOSWL9x"/>
</div>
</div>
</header>
<!-- Content Canvas -->
<div class="mt-16 p-8 flex-1 grid grid-cols-1 lg:grid-cols-2 gap-8 bg-surface">
<!-- Left Panel: Preview -->
<section class="flex flex-col gap-6">
<div class="relative aspect-video rounded-xl overflow-hidden bg-black shadow-[0_8px_32px_rgba(0,26,67,0.06)] group">
<img alt="Video Thumbnail" class="w-full h-full object-cover opacity-80 group-hover:scale-105 transition-transform duration-700" data-alt="Close up of a plate of glistening braised pork belly with rich dark sauce and steam rising" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBqOBtlTnqHpDge0dlMR5Yf0dKokuvgYjzbLSSjWKo91TlJ74j977KjENWfO_dIELSl-j0tIM24OLAWTeE4_G_iKGoec6JIF8vpVaiNYjJb9B6eFKqiSpmsU-tmImBRugwJsskuZvAsft0UxdIU9oLLyOZyJvJ_u1zQJUPdAY-MMcs2wdpykq0vxqZJ_UUeflzHBFNjOMHlHeEG6fXoP9wUC-8WM8OdfKqjBp_lxhXJenUB0Nyyfad3a-CU6yQrYyXgYuRygs6jooSz"/>
<div class="absolute inset-0 flex items-center justify-center">
<button class="w-20 h-20 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center text-white border border-white/30 hover:scale-110 transition-transform active:scale-95">
<span class="material-symbols-outlined text-5xl translate-x-1" data-icon="play_arrow" style="font-variation-settings: 'FILL' 1;">play_arrow</span>
</button>
</div>
<div class="absolute bottom-4 left-4 right-4 h-1.5 bg-white/20 rounded-full overflow-hidden">
<div class="h-full w-1/3 bg-primary shadow-[0_0_8px_rgba(0,87,194,0.6)]"></div>
</div>
</div>
<div class="bg-surface-container-lowest p-8 rounded-xl shadow-[0_8px_32px_rgba(0,26,67,0.06)]">
<h2 class="text-xl font-headline font-bold mb-6 text-on-surface">视频原始信息</h2>
<div class="grid grid-cols-2 gap-y-6 gap-x-4">
<div class="flex flex-col gap-1">
<span class="text-xs text-on-surface-variant font-label uppercase tracking-wider">文件名</span>
<span class="text-sm font-medium text-on-surface truncate">红烧肉制作教程.mp4</span>
</div>
<div class="flex flex-col gap-1">
<span class="text-xs text-on-surface-variant font-label uppercase tracking-wider">格式</span>
<span class="text-sm font-medium text-on-surface">MP4 (H.264)</span>
</div>
<div class="flex flex-col gap-1">
<span class="text-xs text-on-surface-variant font-label uppercase tracking-wider">文件大小</span>
<span class="text-sm font-medium text-on-surface">1.2 GB</span>
</div>
<div class="flex flex-col gap-1">
<span class="text-xs text-on-surface-variant font-label uppercase tracking-wider">时长</span>
<span class="text-sm font-medium text-on-surface">12:34</span>
</div>
</div>
</div>
</section>
<!-- Right Panel: Metadata Editor -->
<section class="bg-surface-container-lowest rounded-xl shadow-[0_8px_32px_rgba(0,26,67,0.06)] flex flex-col">
<div class="p-8 flex-1 overflow-y-auto">
<div class="flex items-center justify-between mb-8">
<h2 class="text-2xl font-headline font-extrabold text-on-surface tracking-tight">内容元数据编辑</h2>
<span class="px-3 py-1 bg-secondary-fixed text-on-secondary-fixed text-xs font-bold rounded-full flex items-center gap-1.5">
<span class="material-symbols-outlined text-[16px]" data-icon="auto_awesome" style="font-variation-settings: 'FILL' 1;">auto_awesome</span>
                            AI 建议已生成
                        </span>
</div>
<div class="space-y-8">
<!-- Title Field -->
<div class="flex flex-col gap-2">
<div class="flex justify-between items-end">
<label class="text-sm font-bold text-on-surface-variant">视频标题</label>
<span class="text-[11px] text-outline">42 / 100</span>
</div>
<input class="w-full bg-surface-container-low border-none rounded-lg p-4 text-sm font-medium text-on-surface focus:ring-2 focus:ring-primary/40 transition-all" type="text" value="最正宗的家常红烧肉做法，肥而不腻入口即化"/>
<p class="text-[11px] text-primary flex items-center gap-1 mt-1">
<span class="material-symbols-outlined text-sm" data-icon="info">info</span>
                                YouTube 标题建议 60 字符以内，以获得更好的 SEO 排名
                            </p>
</div>
<!-- Description Field -->
<div class="flex flex-col gap-2">
<div class="flex justify-between items-end">
<label class="text-sm font-bold text-on-surface-variant">视频描述</label>
<span class="text-[11px] text-outline">350 / 5000</span>
</div>
<textarea class="w-full bg-surface-container-low border-none rounded-lg p-4 text-sm leading-relaxed font-body text-on-surface focus:ring-2 focus:ring-primary/40 transition-all resize-none" rows="6">在这个视频中，我将分享家传三代的红烧肉秘方。不加一滴油，利用猪肉本身的油脂煸炒出香味。

我们会详细讲解：
1. 如何挑选上好的五花肉
2. 去腥的三大关键步骤
3. 炒糖色的火候掌握
4. 小火慢炖的时间配比

跟着我的步骤，保证你也能做出像餐厅一样红亮诱人的红烧肉！</textarea>
</div>
<!-- Tags Field -->
<div class="flex flex-col gap-3">
<label class="text-sm font-bold text-on-surface-variant">搜索标签</label>
<div class="flex flex-wrap gap-2 p-4 bg-surface-container-low rounded-lg min-h-[100px] items-start content-start">
<span class="px-3 py-1.5 bg-tertiary-fixed text-on-tertiary-fixed text-xs font-medium rounded-full flex items-center gap-2 group transition-all hover:bg-tertiary-fixed-dim cursor-default">
                                    美食
                                    <span class="material-symbols-outlined text-sm cursor-pointer hover:text-error transition-colors" data-icon="close">close</span>
</span>
<span class="px-3 py-1.5 bg-tertiary-fixed text-on-tertiary-fixed text-xs font-medium rounded-full flex items-center gap-2 group transition-all hover:bg-tertiary-fixed-dim cursor-default">
                                    红烧肉
                                    <span class="material-symbols-outlined text-sm cursor-pointer hover:text-error transition-colors" data-icon="close">close</span>
</span>
<span class="px-3 py-1.5 bg-tertiary-fixed text-on-tertiary-fixed text-xs font-medium rounded-full flex items-center gap-2 group transition-all hover:bg-tertiary-fixed-dim cursor-default">
                                    中华料理
                                    <span class="material-symbols-outlined text-sm cursor-pointer hover:text-error transition-colors" data-icon="close">close</span>
</span>
<span class="px-3 py-1.5 bg-tertiary-fixed text-on-tertiary-fixed text-xs font-medium rounded-full flex items-center gap-2 group transition-all hover:bg-tertiary-fixed-dim cursor-default">
                                    烹饪教程
                                    <span class="material-symbols-outlined text-sm cursor-pointer hover:text-error transition-colors" data-icon="close">close</span>
</span>
<input class="bg-transparent border-none focus:ring-0 p-0 text-xs w-24 text-on-surface placeholder:text-outline" placeholder="添加标签..." type="text"/>
</div>
</div>
</div>
</div>
<!-- Footer Actions -->
<div class="p-8 pt-0 mt-auto">
<div class="h-px bg-surface-container-high w-full mb-8"></div>
<div class="flex items-center justify-between">
<button class="flex items-center gap-2 text-primary text-sm font-semibold hover:opacity-80 transition-all">
<span class="material-symbols-outlined" data-icon="refresh">refresh</span>
                            重新生成
                        </button>
<div class="flex gap-4">
<button class="px-6 py-2.5 rounded-lg border border-outline-variant text-on-surface-variant font-bold text-sm hover:bg-surface-container-low transition-all active:scale-95">
                                保存草稿
                            </button>
<button class="px-8 py-2.5 rounded-lg bg-gradient-to-r from-primary to-primary-container text-white font-bold text-sm shadow-[0_4px_12px_rgba(0,87,194,0.25)] hover:shadow-[0_6px_16px_rgba(0,87,194,0.35)] transition-all active:scale-95">
                                确认元数据
                            </button>
</div>
</div>
</div>
</section>
</div>
</main>
</body></html>
```
