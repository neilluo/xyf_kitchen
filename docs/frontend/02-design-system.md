# 设计系统

> 依赖文档：[01-tech-stack-and-conventions.md](./01-tech-stack-and-conventions.md) | 被依赖：所有页面文档 04-10
> 设计稿来源：`ui/stitch_grace_video_management/grace_minimalist/DESIGN.md`
> Tailwind token 来源：各 HTML 原型中统一的 `tailwind.config`

## 1. 设计原则

### 1.1 创意北极星："The Digital Curator"

Grace 服务美食博主和非技术创作者，UI 应超越"SaaS 工具"范畴，呈现"高端编辑杂志"质感。核心原则：

- **呼吸感**：大量留白，内容之间充足间距
- **层次感**：通过背景色差异而非线条定义区域
- **触感**：渐变 CTA 按钮，hover 状态有质感变化

### 1.2 "No-Line" 规则

**严格禁止使用 1px solid 边框进行区域分隔。** 替代方案：
- 通过背景色切换定义边界（如 `surface-container-low` 区块置于 `surface` 之上）
- 使用空白作为结构性分隔元素
- 仅在高密度表格中，可使用 `outline-variant` 15% 透明度作为"幽灵边框"

### 1.3 Surface 层级体系

将 UI 视为层叠的纸张：

| 层级 | Token | 色值 | 用途 |
|------|-------|------|------|
| 基底层 | `surface` | #f9f9f9 | 页面主背景 |
| 区块层 | `surface-container-low` | #f3f3f3 | 大布局块（筛选栏、侧边信息区） |
| 内容层 | `surface-container-lowest` | #ffffff | 卡片、主内容容器（从灰色基底"浮起"） |
| 嵌套层 | `surface-container-high` | #e8e8e8 | 卡片内子区块（标签、嵌入信息） |
| 容器层 | `surface-container` | #eeeeee | 通用容器背景 |
| 最高层 | `surface-container-highest` | #e2e2e2 | 进度条轨道、输入框背景 |

### 1.4 渐变 CTA 规则

主要操作按钮使用渐变填充：
```
background: linear-gradient(to right, #0057c2, #006ef2);
/* Tailwind: bg-gradient-to-r from-primary to-primary-container */
```

### 1.5 阴影规则

- **静态元素**：不使用阴影，依靠 Surface 层级体系创造深度
- **浮动元素**（下拉菜单、弹窗）：`0 8px 32px rgba(0, 26, 67, 0.06)`
- **禁止**使用默认 `0 2px 4px` 阴影

### 1.6 毛玻璃效果

用于固定 Header 和浮动覆盖层：
```
background: rgba(255, 255, 255, 0.8);
backdrop-filter: blur(12px);
/* Tailwind: bg-white/80 backdrop-blur-md */
```

## 2. Tailwind 配置

以下为完整的 `tailwind.config.ts`，从 HTML 原型中提取，所有 7 个页面共用同一套 token。

```typescript
// tailwind.config.ts
import type { Config } from 'tailwindcss'

const config: Config = {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // === 主色 ===
        'primary': '#0057c2',
        'primary-container': '#006ef2',
        'primary-fixed': '#d9e2ff',
        'primary-fixed-dim': '#afc6ff',
        'on-primary': '#ffffff',
        'on-primary-container': '#fefcff',
        'on-primary-fixed': '#001a43',
        'on-primary-fixed-variant': '#004398',
        'inverse-primary': '#afc6ff',

        // === 次色 ===
        'secondary': '#4d6077',
        'secondary-container': '#cde1fd',
        'secondary-fixed': '#d1e4ff',
        'secondary-fixed-dim': '#b4c8e3',
        'on-secondary': '#ffffff',
        'on-secondary-container': '#51647c',
        'on-secondary-fixed': '#071d31',
        'on-secondary-fixed-variant': '#35485e',

        // === 强调色 ===
        'tertiary': '#7431d3',
        'tertiary-container': '#8e4fee',
        'tertiary-fixed': '#ecdcff',
        'tertiary-fixed-dim': '#d5baff',
        'on-tertiary': '#ffffff',
        'on-tertiary-container': '#fffbff',
        'on-tertiary-fixed': '#270057',
        'on-tertiary-fixed-variant': '#5e08bd',

        // === 错误色 ===
        'error': '#ba1a1a',
        'error-container': '#ffdad6',
        'on-error': '#ffffff',
        'on-error-container': '#93000a',

        // === Surface 系列 ===
        'surface': '#f9f9f9',
        'surface-bright': '#f9f9f9',
        'surface-dim': '#dadada',
        'surface-variant': '#e2e2e2',
        'surface-tint': '#0059c7',
        'surface-container': '#eeeeee',
        'surface-container-low': '#f3f3f3',
        'surface-container-lowest': '#ffffff',
        'surface-container-high': '#e8e8e8',
        'surface-container-highest': '#e2e2e2',

        // === 文字与边框 ===
        'on-surface': '#1a1c1c',
        'on-surface-variant': '#414755',
        'on-background': '#1a1c1c',
        'outline': '#727786',
        'outline-variant': '#c1c6d7',
        'background': '#f9f9f9',
        'inverse-surface': '#2f3131',
        'inverse-on-surface': '#f1f1f1',
      },

      fontFamily: {
        'headline': ['Manrope', 'sans-serif'],
        'body': ['Inter', 'sans-serif'],
        'label': ['Inter', 'sans-serif'],
      },

      borderRadius: {
        DEFAULT: '0.25rem',  // 4px
        lg: '0.5rem',        // 8px
        xl: '0.75rem',       // 12px
        full: '9999px',
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
}

export default config
```

## 3. 字体与排版

| 层级 | 字体 | 大小 | 权重 | Tailwind 类 | 用途 |
|------|------|------|------|-------------|------|
| Display | Manrope | 2.75rem (44px) | 800 | `font-headline text-[2.75rem] font-extrabold` | Dashboard 问候语 |
| Headline-sm | Manrope | 1.5rem (24px) | 700 | `font-headline text-2xl font-bold` | 页面标题 |
| Title-lg | Manrope | 1.25rem (20px) | 700 | `font-headline text-xl font-bold` | 卡片标题 |
| Title-sm | Inter | 1rem (16px) | 600 | `font-body text-base font-semibold` | 区块标题 |
| Body-md | Inter | 0.875rem (14px) | 400 | `font-body text-sm` | 正文内容 |
| Body-sm | Inter | 0.75rem (12px) | 400 | `font-body text-xs` | 辅助文字 |
| Label-md | Inter | 0.75rem (12px) | 500 | `font-label text-xs font-medium` | 标签、元数据 |
| Label-sm | Inter | 0.6875rem (11px) | 500 | `font-label text-[11px] font-medium` | 极小标注 |

## 4. 图标系统

使用 Google Material Symbols Outlined，统一封装为 `Icon` 组件：

```tsx
// src/components/ui/Icon.tsx
interface IconProps {
  name: string
  className?: string
  size?: number
}

export function Icon({ name, className = '', size = 20 }: IconProps) {
  return (
    <span
      className={`material-symbols-outlined ${className}`}
      style={{ fontSize: size }}
    >
      {name}
    </span>
  )
}
```

图标变体设置（CSS）：
```css
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}
```

### 各页面使用的图标

| 图标名 | 用途 |
|--------|------|
| `dashboard` | 侧边栏 - 仪表盘 |
| `video_library` | 侧边栏 - 视频管理 |
| `fact_check` | 侧边栏 - 元数据审核 |
| `publish` | 侧边栏 - 视频发布 |
| `alt_route` | 侧边栏 - 推广渠道 |
| `task` | 侧边栏 - 推广任务 |
| `settings` | 侧边栏 - 设置 |
| `restaurant_menu` | Logo 图标 |
| `search` | 搜索框 |
| `cloud_upload` | 上传 |
| `play_arrow` | 视频播放 |
| `visibility` | 查看详情 |
| `edit` | 编辑 |
| `send` | 发送/分发 |
| `more_vert` | 更多操作 |
| `auto_awesome` | AI 标记 |
| `check_circle` | 成功/已连接 |
| `error` | 错误 |
| `rocket_launch` | 发布/推广 |
| `open_in_new` | 外部链接 |
| `content_copy` | 复制 |
| `refresh` | 刷新/重新生成 |
| `close` | 关闭/删除标签 |
| `chevron_left` / `chevron_right` | 分页箭头 |

## 5. 原子组件规范

### 5.1 Button

| 变体 | 背景 | 文字色 | 圆角 | Tailwind 类 |
|------|------|--------|------|-------------|
| Primary (渐变 CTA) | `from-primary to-primary-container` | `on-primary` (白) | `rounded-lg` | `bg-gradient-to-r from-primary to-primary-container text-white rounded-lg px-6 py-2.5 font-body text-sm font-medium` |
| Secondary | `surface-container-high` | `on-surface` | `rounded-lg` | `bg-surface-container-high text-on-surface rounded-lg px-4 py-2 font-body text-sm` |
| Ghost | `transparent` | `on-surface-variant` | `rounded-lg` | `text-on-surface-variant hover:bg-surface-container-low rounded-lg px-3 py-2 font-body text-sm` |
| Danger | `error` | `on-error` (白) | `rounded-lg` | `bg-error text-white rounded-lg px-4 py-2 font-body text-sm` |
| Icon Button | `transparent` | `on-surface-variant` | `rounded-full` | `p-2 rounded-full hover:bg-primary/10 text-on-surface-variant` |

### 5.2 StatusBadge

根据状态映射不同背景和文字：

| 状态 | 背景 Token | 文字色 | 示例文字 |
|------|-----------|--------|---------|
| PUBLISHED / 已发布 | `secondary-fixed` (#d1e4ff) | `on-secondary-fixed` | 已发布 |
| READY_TO_PUBLISH / 待发布 | `tertiary-fixed` (#ecdcff) | `on-tertiary-fixed` | 待发布 |
| PUBLISH_FAILED / 发布失败 | `error-container` (#ffdad6) | `on-error-container` | 发布失败 |
| METADATA_GENERATED / 元数据已生成 | `surface-container-high` (#e8e8e8) | `on-surface-variant` | 元数据已生成 |
| UPLOADED / 已上传 | `surface-container-high` (#e8e8e8) | `on-surface-variant` | 已上传 |
| PUBLISHING / 发布中 | `primary-fixed` (#d9e2ff) | `on-primary-fixed` | 发布中 |
| PROMOTION_DONE / 推广完成 | `secondary-fixed` (#d1e4ff) | `on-secondary-fixed` | 推广完成 |
| EXECUTING / 执行中 | `secondary-fixed` (#d1e4ff) | `on-secondary-fixed` | 进行中 (带脉冲动画圆点) |
| COMPLETED / 成功 | `bg-green-100` | `text-green-800` | 成功 |
| FAILED / 失败 | `error-container` (#ffdad6) | `on-error-container` | 失败 |

通用样式：`px-3 py-1 rounded-full text-xs font-medium`

### 5.3 Card

```
基础卡片：bg-surface-container-lowest rounded-lg p-6
    - 不使用 border 和 shadow（遵循 No-Line 规则）
    - 通过背景色差异与基底层产生层次
    - hover 状态（可选）：hover:bg-surface-bright + 环境阴影
```

### 5.4 Input

```
默认状态：bg-surface-container-low rounded-md px-4 py-2.5 text-sm font-body text-on-surface
           placeholder:text-on-surface-variant/50
           无 border
Focus：   ring-2 ring-primary/40
```

### 5.5 Select

与 Input 相同基底样式，附加下拉箭头图标。

### 5.6 TagChip

```
标签芯片：bg-tertiary-fixed text-on-tertiary-fixed-variant px-3 py-1 rounded-full text-xs font-medium
           flex items-center gap-1
可删除：  附加 close 图标按钮（hover:bg-tertiary/20 rounded-full）
```

### 5.7 ProgressBar

```
轨道：h-2 bg-surface-container-highest rounded-full
填充：h-2 bg-primary rounded-full transition-all
容器：w-full
```

### 5.8 Pagination

```
容器：flex items-center gap-2
按钮：w-8 h-8 flex items-center justify-center rounded-lg text-sm
默认：text-on-surface-variant hover:bg-surface-container-low
激活：bg-primary text-white
箭头：chevron_left / chevron_right 图标按钮
```

### 5.9 Toggle (开关)

```
基于 checkbox peer 模式：
<input type="checkbox" class="peer sr-only" />
<div class="w-11 h-6 bg-surface-container-highest rounded-full
            peer-checked:bg-primary transition-colors
            after:content-[''] after:absolute after:top-0.5 after:left-[2px]
            after:bg-white after:rounded-full after:h-5 after:w-5
            after:transition-all peer-checked:after:translate-x-full" />
```

## 6. 侧边栏导航结构

侧边栏宽度 240px，深色背景 `#001529`：

| 图标 | 标签 | 路由 |
|------|------|------|
| `dashboard` | 仪表盘 | `/` |
| `video_library` | 视频管理 | `/videos` |
| `fact_check` | 元数据审核 | `/videos/:videoId/metadata` (列表入口在视频管理) |
| `publish` | 视频发布 | `/videos/:videoId/distribute` (列表入口在视频管理) |
| `alt_route` | 推广渠道 | `/promotions` |
| `task` | 推广任务 | `/promotions` |
| `settings` | 设置 | `/settings` |

导航项样式：

```
默认：    text-slate-400 py-4 px-6 flex items-center gap-3 text-sm font-body
           hover:text-white hover:bg-white/5
激活：    bg-blue-600/10 text-blue-400 border-r-4 border-blue-500
```

## 7. 全局布局

```
┌────────────────────────────────────────────────┐
│  Sidebar (240px, fixed, #001529)  │  Header    │
│                                    │ (fixed,    │
│  Logo                              │  h-16,     │
│  Nav Items                         │  bg-white  │
│                                    │  /80 blur) │
│                                    ├────────────┤
│                                    │            │
│                                    │  Main      │
│                                    │  Content   │
│                                    │  (scroll)  │
│                                    │            │
│  Settings (底部)                   │            │
└────────────────────────────────────────────────┘

主内容区：ml-[240px] pt-24 px-8 pb-8
Header：  fixed top-0 left-[240px] right-0 h-16 bg-white/80 backdrop-blur-md z-10
```
