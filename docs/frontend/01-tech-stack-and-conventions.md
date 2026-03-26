# 技术栈与编码约定

> 依赖文档：本文件 | 前置阅读：无

## 1. 项目初始化

```bash
npm create vite@latest grace-frontend -- --template react-ts
cd grace-frontend
npm install
```

### 核心依赖

```bash
# 路由
npm install react-router-dom@6

# 服务端状态管理
npm install @tanstack/react-query@5

# 客户端状态管理
npm install zustand@4

# HTTP 客户端
npm install axios@1

# 图表
npm install recharts@2

# 样式
npm install -D tailwindcss@3 postcss autoprefixer
npx tailwindcss init -p
```

### 字体与图标

在 `index.html` 的 `<head>` 中引入：

```html
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Manrope:wght@700;800&display=swap" rel="stylesheet" />
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap" rel="stylesheet" />
```

## 2. 项目目录结构

```
grace-frontend/
├── public/
├── src/
│   ├── api/                    # API 请求函数（按领域分文件）
│   │   ├── client.ts           # Axios 实例 + 拦截器
│   │   ├── dashboard.ts        # A1
│   │   ├── video.ts            # B1-B6
│   │   ├── metadata.ts         # C1-C5
│   │   ├── distribution.ts     # D1-D6
│   │   ├── channel.ts          # E1-E5
│   │   ├── promotion.ts        # F1-F5
│   │   └── settings.ts         # G1-G10
│   ├── components/
│   │   ├── layout/             # AppLayout, Sidebar, Header
│   │   │   ├── AppLayout.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   └── Header.tsx
│   │   └── ui/                 # 原子组件
│   │       ├── Button.tsx
│   │       ├── StatusBadge.tsx
│   │       ├── Card.tsx
│   │       ├── Icon.tsx        # Material Symbols 封装
│   │       ├── Input.tsx
│   │       ├── Select.tsx
│   │       ├── TagChip.tsx
│   │       ├── ProgressBar.tsx
│   │       ├── Pagination.tsx
│   │       ├── Toggle.tsx
│   │       └── Table.tsx
│   ├── hooks/                  # React Query hooks（按领域分文件）
│   │   ├── useDashboard.ts
│   │   ├── useVideos.ts
│   │   ├── useMetadata.ts
│   │   ├── useDistribution.ts
│   │   ├── useChannels.ts
│   │   ├── usePromotions.ts
│   │   ├── useSettings.ts
│   │   └── useUpload.ts       # 分片上传 hook
│   ├── pages/                  # 7 个页面组件
│   │   ├── DashboardPage.tsx
│   │   ├── VideoManagementPage.tsx
│   │   ├── VideoUploadPage.tsx
│   │   ├── MetadataReviewPage.tsx
│   │   ├── DistributionPromotionPage.tsx
│   │   ├── PromotionHistoryPage.tsx
│   │   └── SettingsPage.tsx
│   ├── store/                  # Zustand store
│   │   └── useAppStore.ts     # 上传队列 + Toast 通知
│   ├── types/                  # TypeScript 类型定义（按领域分文件）
│   │   ├── common.ts          # ApiResponse, PaginatedResponse, ErrorResponse
│   │   ├── dashboard.ts
│   │   ├── video.ts
│   │   ├── metadata.ts
│   │   ├── distribution.ts
│   │   ├── channel.ts
│   │   ├── promotion.ts
│   │   └── settings.ts
│   ├── utils/                  # 工具函数
│   │   ├── format.ts          # 日期、文件大小、时长格式化
│   │   ├── status.ts          # 状态枚举 → 中文标签 + 颜色映射
│   │   └── constants.ts       # 枚举常量、路由路径
│   ├── App.tsx                 # 路由配置
│   ├── main.tsx               # 入口文件
│   └── index.css              # Tailwind 指令
├── tailwind.config.ts          # 完整设计系统配置（见 02-design-system.md）
├── vite.config.ts
├── tsconfig.json
├── .env                        # 环境变量
└── package.json
```

## 3. 文件命名约定

| 类别 | 约定 | 示例 |
|------|------|------|
| React 组件 | PascalCase，`.tsx` | `StatusBadge.tsx`, `DashboardPage.tsx` |
| Hook | camelCase，`use` 前缀，`.ts` | `useVideos.ts`, `useUpload.ts` |
| API 函数 | camelCase，`.ts` | `video.ts`, `metadata.ts` |
| 类型定义 | camelCase，`.ts` | `video.ts`（在 `types/` 下） |
| 工具函数 | camelCase，`.ts` | `format.ts`, `status.ts` |
| 页面组件 | PascalCase + `Page` 后缀 | `DashboardPage.tsx` |

## 4. 编码约定

### TypeScript
- 严格模式 `"strict": true`
- 优先使用 `interface` 定义对象类型，`type` 用于联合类型和工具类型
- API 响应类型使用泛型 `ApiResponse<T>`
- 枚举使用 `const enum` 或字符串字面量联合类型

### React
- 全部使用函数组件 + Hooks
- 组件 Props 接口命名为 `{ComponentName}Props`
- 事件处理函数命名为 `handle{Event}`（如 `handleSubmit`）
- 避免在组件内定义内联函数，提取为 `useCallback`（仅在必要时）

### 样式
- 全部使用 Tailwind CSS 类名，不写自定义 CSS（除 Tailwind 指令外）
- 动态类名使用模板字符串或 `clsx` 库
- 遵循设计系统规范（详见 `02-design-system.md`）

### API 调用
- 所有 API 调用通过 `src/api/` 下的函数执行
- 组件通过 `src/hooks/` 下的 React Query hooks 消费数据
- 禁止在组件中直接使用 `axios`

## 5. 环境变量

```env
# .env
VITE_API_BASE_URL=http://localhost:8080/api
```

在代码中通过 `import.meta.env.VITE_API_BASE_URL` 访问。

## 6. Vite 配置

```typescript
// vite.config.ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```
