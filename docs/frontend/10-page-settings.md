# 页面：设置 (Settings)

> 依赖文档：[02-design-system.md](./02-design-system.md)、[03-shared-infrastructure.md](./03-shared-infrastructure.md)
> 设计稿：`ui/stitch_grace_video_management/grace_settings_page_updated_sidebar/screen.png`
> HTML 原型：`ui/stitch_grace_video_management/grace_settings_page_updated_sidebar/code.html`
> 路由：`/settings`

## A. 页面概览

设置页面采用 12 列网格布局（左 7 右 5），管理用户资料、已连接第三方平台账户、通知偏好和 API Key。左侧为主要设置项，右侧为 API 管理和提示卡片。

## B. 组件层级树

```
SettingsPage
├── PageHeader ("设置" 标题)
└── ContentGrid (grid-cols-12 gap-8)
    ├── LeftColumn (col-span-7)
    │   ├── ProfileSection
    │   │   ├── SectionTitle ("用户资料")
    │   │   └── ProfileCard
    │   │       ├── AvatarSection
    │   │       │   ├── Avatar (w-32 h-32 rounded-full, ring-4 ring-surface-container-low)
    │   │       │   ├── CameraOverlay (绝对定位, 相机图标)
    │   │       │   └── ButtonGroup
    │   │       │       ├── ChangeAvatarButton
    │   │       │       └── EditProfileButton
    │   │       └── ProfileForm (displayName, email)
    │   ├── ConnectedAccountsSection
    │   │   ├── SectionTitle ("已连接账户")
    │   │   └── AccountList
    │   │       └── AccountRow × N
    │   │           ├── PlatformIcon (圆形图标)
    │   │           ├── PlatformName
    │   │           ├── AccountName (已连接时)
    │   │           └── StatusButton
    │   │               ├── Connected: check_circle 绿色 + "已连接"
    │   │               └── Not Connected: "连接" 按钮 (bg-primary-fixed)
    │   └── NotificationSection
    │       ├── SectionTitle ("通知偏好")
    │       └── NotificationList
    │           └── NotificationRow × 3
    │               ├── Label + Description
    │               └── Toggle (开关组件)
    └── RightColumn (col-span-5)
        ├── ApiKeySection
        │   ├── SectionTitle ("API 管理")
        │   └── ApiKeyCard
        │       ├── CurrentKey
        │       │   ├── MaskedDisplay ("••••••••••••abcd")
        │       │   ├── CopyButton (content_copy)
        │       │   └── RevealButton (visibility)
        │       ├── KeyInfo (过期时间、最后使用时间)
        │       └── RegenerateButton
        └── ProTipCard
            ├── TipIcon (auto_awesome)
            ├── TipTitle
            └── TipContent
```

## C. API 端点

| 端点 | 方法 | Hook | 调用时机 |
|------|------|------|----------|
| G1. `/api/settings/profile` | GET | `useProfile()` | 页面加载 |
| G2. `/api/settings/profile` | PUT | `useUpdateProfile()` | 保存资料 |
| G3. `/api/settings/profile/avatar` | POST | `useUploadAvatar()` | 上传头像 |
| G4. `/api/settings/connected-accounts` | GET | `useConnectedAccounts()` | 页面加载 |
| G5. `/api/settings/connected-accounts/{platform}` | DELETE | `useDisconnectAccount()` | 断开连接 |
| D3. `/api/distribution/auth/{platform}` | POST | - | 发起 OAuth 连接 |
| G6. `/api/settings/notifications` | GET | `useNotifications()` | 页面加载 |
| G7. `/api/settings/notifications` | PUT | `useUpdateNotifications()` | 切换开关 |
| G8. `/api/settings/api-keys` | POST | `useCreateApiKey()` | 生成 API Key |
| G9. `/api/settings/api-keys` | GET | `useApiKeys()` | 页面加载 |
| G10. `/api/settings/api-keys/{id}` | DELETE | `useDeleteApiKey()` | 撤销 Key |
| E4. `/api/channels` | GET | `useChannelList()` | 推广渠道管理（如需在设置中展示） |

### 数据类型

```typescript
interface UserProfile {
  userId: string
  displayName: string
  email: string | null
  avatarUrl: string | null
  createdAt: string
}

interface ConnectedAccount {
  platform: string
  displayName: string
  authorized: boolean
  accountName: string | null
  connectedAt: string | null
}

interface NotificationPreferences {
  uploadComplete: boolean
  promotionSuccess: boolean
  systemUpdates: boolean
}

interface ApiKey {
  apiKeyId: string
  name: string
  key?: string       // 仅创建时返回
  prefix: string
  expiresAt: string
  lastUsedAt: string | null
  createdAt: string
}
```

## D. 状态管理

| 类型 | 内容 | 管理方式 |
|------|------|----------|
| 服务端状态 | 用户资料 | React Query `useProfile` |
| 服务端状态 | 已连接账户列表 | React Query `useConnectedAccounts` |
| 服务端状态 | 通知偏好 | React Query `useNotifications` |
| 服务端状态 | API Key 列表 | React Query `useApiKeys` |
| 本地状态 | 资料编辑表单 | `useState` |
| 本地状态 | 编辑模式 (isEditing) | `useState<boolean>` |
| 本地状态 | 新创建的 Key 明文 (一次性显示) | `useState<string \| null>` |
| 本地状态 | Key 可见状态 (isRevealed) | `useState<boolean>` |

## E. 关键交互

| 用户操作 | 触发行为 | 状态变更 | UI 反馈 |
|---------|---------|---------|---------|
| 点击 Change Avatar | 打开文件选择器 | - | 系统文件对话框 |
| 选择头像文件 | 调用 G3 上传 | avatarUrl 更新 | 头像刷新 |
| 点击 Edit Profile | 进入编辑模式 | isEditing = true | 表单可编辑 |
| 保存资料 | 调用 G2 | isEditing = false | Toast "保存成功" |
| 点击"连接"平台 | 调用 D3 获取 OAuth URL | - | 跳转到 OAuth 页面 |
| OAuth 回调返回 | 刷新已连接账户列表 | - | 状态更新为已连接 |
| 点击"断开连接" | 确认弹窗 → 调用 G5 | 账户状态更新 | 状态变为未连接 |
| 切换通知开关 | 调用 G7 (乐观更新) | 即时切换 UI | 开关动画 |
| 点击"生成 API Key" | 调用 G8 | 新 Key 显示 | 一次性显示明文 Key |
| 点击复制 Key | 复制到剪贴板 | - | Toast "已复制" |
| 点击 Reveal/Hide | 切换 Key 可见性 | isRevealed toggle | 显示/隐藏明文 |
| 点击 Regenerate | 确认弹窗 → 调用 G10 删除旧 + G8 创建新 | Key 更新 | 新 Key 显示 |

## F. 错误处理

| 场景 | 错误码 | 用户提示 | 恢复操作 |
|------|--------|---------|---------|
| 资料不存在 | 5001 | "用户资料不存在" | - |
| Key 不存在 | 5002 | "API Key 不存在" | 刷新列表 |
| 头像过大 | - | "头像文件不能超过 2MB" | 客户端校验 |
| 头像格式错误 | - | "仅支持 JPG/PNG 格式" | 客户端校验 |
| OAuth 失败 | 3007 | "平台授权失败" | 提供重试按钮 |
| 平台未授权 | 3003 | "该平台未授权" | - |

## G. 视觉实现备注

### 页面布局

```
容器：grid grid-cols-12 gap-8
左栏：col-span-7, 各区块间距 space-y-8
右栏：col-span-5, 各区块间距 space-y-8
```

### ProfileCard

```
容器：bg-surface-container-lowest rounded-lg p-8
头像区域：flex items-start gap-8
  头像容器：relative
    图片：w-32 h-32 rounded-full object-cover
          ring-4 ring-surface-container-low
    相机覆层：absolute bottom-0 right-0
              w-8 h-8 rounded-full bg-primary flex items-center justify-center
              photo_camera 图标 text-white size=16
  信息区：flex-1
    名字：font-headline text-xl font-bold text-on-surface
    邮箱：font-body text-sm text-on-surface-variant mt-1
    按钮组：flex gap-3 mt-4
      Change Avatar：bg-surface-container-high text-on-surface rounded-lg px-4 py-2 text-sm
      Edit Profile：bg-gradient-to-r from-primary to-primary-container text-white rounded-lg px-4 py-2 text-sm
```

### ConnectedAccountRow

```
容器：flex items-center gap-4 py-4
      （行间用 bg 色差分隔，不用 border）
平台图标：
  w-10 h-10 rounded-full flex items-center justify-center
  YouTube: bg-red-100, YouTube SVG
  Weibo: bg-yellow-100
  Bilibili: bg-blue-100
平台名：font-body text-sm font-medium text-on-surface flex-1
账户名：font-body text-xs text-on-surface-variant
状态按钮：
  已连接：flex items-center gap-1 text-green-600 text-sm
          check_circle 图标 size=16 + "已连接"
  未连接：bg-primary-fixed text-on-primary-fixed rounded-lg px-4 py-1.5 text-sm
          "连接"
```

### NotificationRow

```
容器：flex items-center justify-between py-4
左侧：
  标签：font-body text-sm font-medium text-on-surface
  描述：font-body text-xs text-on-surface-variant mt-0.5
右侧：Toggle 组件（见 02-design-system.md 5.9 节）
```

### ApiKeyCard

```
容器：bg-surface-container-lowest rounded-lg p-6
密钥显示：
  容器：flex items-center gap-3 bg-surface-container-low rounded-lg px-4 py-3
  密钥文字：font-mono text-sm text-on-surface flex-1
            隐藏状态："••••••••••••abcd"
            显示状态：完整密钥前缀
  复制按钮：p-2 rounded-lg hover:bg-surface-container-high
            content_copy 图标 size=18
  显示/隐藏：p-2 rounded-lg hover:bg-surface-container-high
             visibility / visibility_off 图标 size=18
信息：mt-4 grid grid-cols-2 gap-4
  过期时间：font-label text-xs text-on-surface-variant
  最后使用：font-label text-xs text-on-surface-variant
Regenerate 按钮：
  mt-4 w-full bg-surface-container-high text-on-surface rounded-lg py-2.5
  text-sm font-medium text-center
  flex items-center justify-center gap-2
  refresh 图标
```

### ProTipCard

```
容器：bg-tertiary-fixed rounded-lg p-6
图标：auto_awesome text-tertiary size=24 mb-3
标题：font-headline text-base font-bold text-on-tertiary-fixed
内容：font-body text-sm text-on-tertiary-fixed-variant mt-2
装饰：右上角大号半透明图标 text-tertiary/10 size=64 absolute
```


## H. HTML 原型参考代码

> 此 HTML 原型使用 CDN Tailwind CSS 构建，包含页面的完整 DOM 结构和精确的 Tailwind class。开发时请参考其中的布局结构、颜色 token、间距和组件样式。

> 源文件：`ui/stitch_grace_video_management/grace_settings_page_updated_sidebar/code.html`

```html
<!DOCTYPE html>

<html class="light" lang="en"><head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<title>Settings | Grace - The Culinary Curator</title>
<script src="https://cdn.tailwindcss.com?plugins=forms,container-queries"></script>
<link href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;600;700;800&amp;family=Inter:wght@400;500;600&amp;display=swap" rel="stylesheet"/>
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
<script id="tailwind-config">
        tailwind.config = {
            darkMode: "class",
            theme: {
                extend: {
                    colors: {
                        "surface-container-high": "#e8e8e8",
                        "on-primary-fixed-variant": "#004398",
                        "on-secondary-container": "#51647c",
                        "on-primary-container": "#fefcff",
                        "surface-container-lowest": "#ffffff",
                        "secondary-container": "#cde1fd",
                        "inverse-surface": "#2f3131",
                        "on-tertiary-fixed": "#270057",
                        "surface": "#f9f9f9",
                        "primary-fixed-dim": "#afc6ff",
                        "surface-bright": "#f9f9f9",
                        "on-surface": "#1a1c1c",
                        "on-secondary": "#ffffff",
                        "secondary-fixed-dim": "#b4c8e3",
                        "on-secondary-fixed-variant": "#35485e",
                        "on-primary-fixed": "#001a43",
                        "surface-variant": "#e2e2e2",
                        "on-secondary-fixed": "#071d31",
                        "on-background": "#1a1c1c",
                        "inverse-on-surface": "#f1f1f1",
                        "tertiary-fixed": "#ecdcff",
                        "surface-dim": "#dadada",
                        "primary": "#0057c2",
                        "on-tertiary-fixed-variant": "#5e08bd",
                        "on-error-container": "#93000a",
                        "tertiary-container": "#8e4fee",
                        "outline-variant": "#c1c6d7",
                        "error-container": "#ffdad6",
                        "surface-tint": "#0059c7",
                        "on-surface-variant": "#414755",
                        "inverse-primary": "#afc6ff",
                        "error": "#ba1a1a",
                        "on-tertiary-container": "#fffbff",
                        "outline": "#727786",
                        "on-primary": "#ffffff",
                        "background": "#f9f9f9",
                        "tertiary-fixed-dim": "#d5baff",
                        "tertiary": "#7431d3",
                        "secondary": "#4d6077",
                        "on-error": "#ffffff",
                        "on-tertiary": "#ffffff",
                        "surface-container-low": "#f3f3f3",
                        "primary-fixed": "#d9e2ff",
                        "surface-container": "#eeeeee",
                        "secondary-fixed": "#d1e4ff",
                        "primary-container": "#006ef2",
                        "surface-container-highest": "#e2e2e2"
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
            vertical-align: middle;
        }
        body { font-family: 'Inter', sans-serif; }
        h1, h2, h3 { font-family: 'Manrope', sans-serif; }
    </style>
</head>
<body class="bg-surface text-on-surface min-h-screen">
<!-- SideNavBar (Authority: JSON - Updated to match Dashboard Style) -->
<aside class="h-screen w-[240px] fixed left-0 top-0 bg-[#001529] flex flex-col justify-between pb-8 z-50 shadow-[0_8px_32px_rgba(0,26,67,0.06)]">
<div>
<div class="py-8 px-6 text-white text-2xl font-bold flex items-center gap-2 font-headline">
<span class="material-symbols-outlined text-blue-400">restaurant_menu</span>
                Grace
            </div>
<nav class="mt-4 flex flex-col">
<a class="text-slate-400 hover:text-white py-4 px-6 flex items-center gap-3 transition-colors hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined">dashboard</span>
                    仪表盘
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 flex items-center gap-3 transition-colors hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined">video_library</span>
                    视频管理
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 flex items-center gap-3 transition-colors hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined">fact_check</span>
                    元数据审核
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 flex items-center gap-3 transition-colors hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined">publish</span>
                    视频发布
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 flex items-center gap-3 transition-colors hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined">alt_route</span>
                    推广渠道
                </a>
<a class="text-slate-400 hover:text-white py-4 px-6 flex items-center gap-3 transition-colors hover:bg-white/5 font-body text-sm" href="#">
<span class="material-symbols-outlined">task</span>
                    推广任务
                </a>
</nav>
</div>
<div class="px-0">
<!-- Active Tab: Settings (bottom position matching other screens) -->
<a class="bg-blue-600/10 text-blue-400 border-r-4 border-blue-500 py-4 px-6 flex items-center gap-3 transition-all font-body text-sm" href="#">
<span class="material-symbols-outlined">settings</span>
                设置
            </a>
</div>
</aside>
<!-- TopAppBar (Authority: JSON - Adjusted width to match new sidebar) -->
<header class="h-20 fixed top-0 right-0 left-[240px] w-[calc(100%-240px)] bg-white/80 backdrop-blur-md flex justify-between items-center px-8 z-40">
<div class="flex items-center gap-2 font-inter text-sm font-medium">
<span class="text-slate-400">主页</span>
<span class="text-slate-300">/</span>
<span class="text-blue-600 font-semibold">设置</span>
</div>
<div class="flex items-center gap-6">
<div class="relative group">
<span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline">search</span>
<input class="bg-surface-container-low border-none rounded-lg pl-10 pr-4 py-2 text-sm focus:ring-2 focus:ring-primary/40 w-64 transition-all" placeholder="Search settings..." type="text"/>
</div>
<div class="flex items-center gap-4">
<button class="text-slate-500 hover:bg-slate-50 p-2 rounded-lg transition-all duration-200 relative">
<span class="material-symbols-outlined">notifications</span>
<span class="absolute top-2 right-2 w-2 h-2 bg-error rounded-full border-2 border-white"></span>
</button>
<div class="flex items-center gap-3 cursor-pointer hover:bg-slate-50 p-1 pr-3 rounded-lg transition-all">
<img alt="User Avatar" class="w-8 h-8 rounded-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuBLZDP-fSs2RBbC959OgYltepawshtxwUrw_bVwaSWbwwBrGnCfqQKn_RS-pbowjFkUviPd0tn-FsD8QjsAuFVKGgDShcIuaOBzkx7f4X5NIW7OUAi2Yq1yGxMrkBQnfl7oa6jIbkxOrxZFZNelPPeqw-MtQqQJXjUc5wYgmXZHfUJSULtqKJeHTGGFuY70cxER-zJLcpBeMRfe4wy49TErv2Clz3Vc_E2QFNcE2LZ4lEzmJRYkta4cAhHu7MgnzhLNkkI-iB2asjHW"/>
<span class="text-sm font-medium text-on-surface">Grace Curator</span>
</div>
</div>
</div>
</header>
<!-- Main Content Canvas -->
<main class="ml-[240px] pt-24 pb-12 px-8 min-h-screen">
<div class="max-w-7xl mx-auto">
<div class="mb-10">
<h1 class="text-4xl font-extrabold tracking-tight text-on-surface font-headline mb-2">Settings</h1>
</div>
<div class="grid grid-cols-12 gap-8">
<!-- Left Column: Profile & Accounts -->
<div class="col-span-12 lg:col-span-7 flex flex-col gap-8">
<!-- 1. User Profile Section -->
<section class="bg-surface-container-lowest rounded-xl p-8 shadow-[0_8px_32px_rgba(0,26,67,0.04)] border-none">
<div class="flex items-center justify-between mb-8">
<h3 class="text-xl font-bold font-manrope flex items-center gap-2">
<span class="material-symbols-outlined text-primary">person</span>
                                User Profile
                            </h3>
</div>
<div class="flex items-center gap-8">
<div class="relative group">
<img alt="Grace Chef Avatar" class="w-32 h-32 rounded-full object-cover ring-4 ring-surface-container-low" data-alt="Close-up portrait of a professional chef in white uniform with blurred kitchen background and soft natural lighting" src="https://lh3.googleusercontent.com/aida-public/AB6AXuAkWCDJaGWJPrk0VQo6lOPO_RvOrF0BxqXzbXkT35KluMZYcOw7gpRi9Ys97JXZfcqGXKKasGt1y_jc1931G9iNKRqY2TrNTddLIwORBTiuP9J_zIgQ2MML78RgcWx6EPJJlCqOOqMjMWPllmP1pOJEIHCff9wDDidp3zjRIxW229SLvFSewZ2U0ADeDg-u5ZUYRae_2F_HfuGy7ptnheYSUHawQp1_3ZFCGj5SayPyz9L4lhGA8tkbEHUbmIWMCR0gak039VDIORye"/>
<button class="absolute bottom-0 right-0 bg-primary text-white p-2 rounded-full shadow-lg hover:scale-110 transition-transform">
<span class="material-symbols-outlined text-sm">photo_camera</span>
</button>
</div>
<div class="flex-1">
<h4 class="text-2xl font-extrabold text-on-surface">Grace Chef</h4>
<p class="text-secondary font-medium mb-4">grace.chef@theculinarycurator.com</p>
<div class="flex gap-3">
<button class="bg-primary text-white px-5 py-2 rounded-lg text-sm font-bold shadow-sm hover:opacity-90 transition-opacity">
                                        Change Avatar
                                    </button>
<button class="bg-surface-container-high text-on-surface px-5 py-2 rounded-lg text-sm font-bold hover:bg-surface-variant transition-colors">
                                        Edit Profile
                                    </button>
</div>
</div>
</div>
</section>
<!-- 2. Connected Accounts Section -->
<section class="bg-surface-container-lowest rounded-xl p-8 shadow-[0_8px_32px_rgba(0,26,67,0.04)] border-none">
<h3 class="text-xl font-bold font-manrope mb-8 flex items-center gap-2">
<span class="material-symbols-outlined text-primary">link</span>
                            Connected Accounts
                        </h3>
<div class="space-y-4">
<!-- YouTube -->
<div class="flex items-center justify-between p-4 bg-surface rounded-lg">
<div class="flex items-center gap-4">
<div class="w-10 h-10 rounded-full bg-red-50 flex items-center justify-center">
<span class="material-symbols-outlined text-red-600">video_call</span>
</div>
<div>
<p class="font-bold">YouTube</p>
<p class="text-xs text-secondary">Grace Kitchen Official</p>
</div>
</div>
<div class="flex items-center gap-2 text-emerald-600 font-semibold text-sm">
<span class="material-symbols-outlined text-sm" style="font-variation-settings: 'FILL' 1;">check_circle</span>
                                    Connected
                                </div>
</div>
<!-- Weibo -->
<div class="flex items-center justify-between p-4 bg-surface rounded-lg">
<div class="flex items-center gap-4">
<div class="w-10 h-10 rounded-full bg-orange-50 flex items-center justify-center">
<span class="material-symbols-outlined text-orange-600">public</span>
</div>
<div>
<p class="font-bold">Weibo</p>
<p class="text-xs text-secondary">Not connected</p>
</div>
</div>
<button class="bg-primary-fixed text-on-primary-fixed px-4 py-2 rounded-lg text-sm font-bold hover:bg-primary-fixed-dim transition-colors">
                                    Connect
                                </button>
</div>
<!-- Bilibili -->
<div class="flex items-center justify-between p-4 bg-surface rounded-lg">
<div class="flex items-center gap-4">
<div class="w-10 h-10 rounded-full bg-sky-50 flex items-center justify-center">
<span class="material-symbols-outlined text-sky-500">tv</span>
</div>
<div>
<p class="font-bold">Bilibili</p>
<p class="text-xs text-secondary">Not connected</p>
</div>
</div>
<button class="bg-primary-fixed text-on-primary-fixed px-4 py-2 rounded-lg text-sm font-bold hover:bg-primary-fixed-dim transition-colors">
                                    Connect
                                </button>
</div>
</div>
</section>
</div>
<!-- Right Column: Notifications & API -->
<div class="col-span-12 lg:col-span-5 flex flex-col gap-8">
<!-- 3. Notification Preferences -->
<section class="bg-surface-container-lowest rounded-xl p-8 shadow-[0_8px_32px_rgba(0,26,67,0.04)] border-none">
<h3 class="text-xl font-bold font-manrope mb-8 flex items-center gap-2">
<span class="material-symbols-outlined text-primary">notifications_active</span>
                            Notification Preferences
                        </h3>
<div class="space-y-6">
<div class="flex items-center justify-between">
<div>
<p class="font-bold text-sm">Upload complete</p>
<p class="text-xs text-secondary">Get notified when video processing finishes</p>
</div>
<label class="relative inline-flex items-center cursor-pointer">
<input checked="" class="sr-only peer" type="checkbox"/>
<div class="w-11 h-6 bg-surface-container-high peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary"></div>
</label>
</div>
<div class="flex items-center justify-between">
<div>
<p class="font-bold text-sm">Promotion success</p>
<p class="text-xs text-secondary">Alerts for reaching view targets</p>
</div>
<label class="relative inline-flex items-center cursor-pointer">
<input checked="" class="sr-only peer" type="checkbox"/>
<div class="w-11 h-6 bg-surface-container-high peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary"></div>
</label>
</div>
<div class="flex items-center justify-between">
<div>
<p class="font-bold text-sm">System updates</p>
<p class="text-xs text-secondary">New features and maintenance notes</p>
</div>
<label class="relative inline-flex items-center cursor-pointer">
<input class="sr-only peer" type="checkbox"/>
<div class="w-11 h-6 bg-surface-container-high peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary"></div>
</label>
</div>
</div>
</section>
<!-- 4. API Management Section -->
<section class="bg-surface-container-lowest rounded-xl p-8 shadow-[0_8px_32px_rgba(0,26,67,0.04)] border-none">
<h3 class="text-xl font-bold font-manrope mb-8 flex items-center gap-2">
<span class="material-symbols-outlined text-primary">key</span>
                            API Management
                        </h3>
<div class="bg-surface p-4 rounded-lg border-l-4 border-primary mb-6">
<p class="text-xs font-bold text-primary uppercase tracking-widest mb-1">Primary API Key</p>
<div class="flex items-center justify-between">
<code class="text-lg font-mono tracking-tighter text-on-surface-variant">••••••••••••abcd</code>
<div class="flex gap-2">
<button class="p-2 hover:bg-surface-container-high rounded-md transition-colors" title="Copy Key">
<span class="material-symbols-outlined text-sm">content_copy</span>
</button>
<button class="p-2 hover:bg-surface-container-high rounded-md transition-colors" title="Reveal">
<span class="material-symbols-outlined text-sm">visibility</span>
</button>
</div>
</div>
</div>
<button class="w-full bg-surface-container-high text-on-surface-variant font-bold py-3 rounded-lg hover:bg-surface-variant transition-colors flex items-center justify-center gap-2">
<span class="material-symbols-outlined text-sm">refresh</span>
                            Regenerate API Key
                        </button>
<p class="text-[10px] text-secondary mt-4 text-center">Last rotated: Oct 12, 2023</p>
</section>
<!-- Helpful Tip -->
<div class="bg-tertiary-fixed p-6 rounded-xl relative overflow-hidden group">
<div class="relative z-10">
<h4 class="font-manrope font-extrabold text-on-tertiary-fixed mb-2">Pro Curator Tip</h4>
<p class="text-xs text-on-tertiary-fixed-variant leading-relaxed">
                                Connecting all your accounts allows Grace to synchronize publishing schedules and optimize engagement across time zones automatically.
                            </p>
</div>
<span class="material-symbols-outlined absolute -bottom-4 -right-4 text-8xl text-on-tertiary-fixed/10 group-hover:scale-110 transition-transform duration-700">tips_and_updates</span>
</div>
</div>
</div>
</div>
</main>
</body></html>
```
