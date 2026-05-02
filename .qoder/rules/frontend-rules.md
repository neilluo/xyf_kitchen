---
trigger: "**/*.{ts,tsx}"
---

# Frontend Rules (TypeScript/React)

## Self-Built UI Only — No Third-Party Libraries
**Do**: 所有 UI 组件自建于 `src/components/ui/`，使用 Tailwind CSS 原子类。
```typescript
const StatusBadge = ({ status, size = 'md' }: StatusBadgeProps) => (
  <span className={`rounded px-2 py-0.5 text-xs font-medium ${COLORS[status]} ${SIZES[size]}`}>
    {status}
  </span>
);
```
**Don't**: 使用 Ant Design / MUI / shadcn / 任何第三方 UI 库。
```typescript
// 错误 — 引入第三方 UI 库
import { Badge } from 'antd';
import { Button } from '@mui/material';
```
**Self-check**: import 语句中不出现 `antd`、`@mui`、`@chakra`、`shadcn`。

## Data Consumption Chain — API → Hooks → Components
**Do**: 组件通过自定义 hooks 消费数据，hooks 封装 API 调用。
```typescript
// src/api/video.ts
export const fetchVideos = async (filters) => client.get('/videos', { params: filters });

// src/hooks/useVideos.ts
export const useVideos = (filters) => useQuery({ queryKey: ['videos', filters], queryFn: () => fetchVideos(filters) });

// 组件
const { data, isLoading } = useVideos(filters);
```
**Don't**: 组件内直接调用 axios/fetch。
```typescript
// 错误 — 组件直接调 API
const [videos, setVideos] = useState([]);
useEffect(() => { axios.get('/videos').then(setVideos); }, []);
```
**Self-check**: 组件文件中不出现 `axios`、`fetch`、`apiClient` 的 import。

## Naming Conventions
**Do**: 组件 `PascalCase.tsx`，hooks `useCamelCase.ts`，类型 `camelCase.ts`，事件处理 `handle{Event}`。
```typescript
interface VideoPlayerProps { url: string; autoPlay?: boolean; }
const VideoPlayer = ({ url, autoPlay }: VideoPlayerProps) => { ... };
const handleSubmit = useCallback(() => { ... }, []);
```
**Don't**: 通用命名如 `data`、`temp`、`handleClick`（不表达意图）。
**Self-check**: 每个组件文件名与导出组件名一致，每个事件处理器名包含具体动作名。

## TypeScript Strict — No any
**Do**: `strict: true`，优先 `interface`，联合类型用 `type`。
```typescript
interface Video { id: string; status: VideoStatus; tags?: string[]; }
type VideoStatus = 'draft' | 'published' | 'archived';
```
**Don't**: 使用 `as any` 或 `any` 类型绕过类型检查。
```typescript
// 错误 — 类型绕过
const data = response as any;
const processVideo = (v: any) => { ... };
```
**Self-check**: ESLint `no-explicit-any` 规则无报错，代码中无 `any` 关键字。

## Tailwind Only — No Custom CSS
**Do**: 全部使用 Tailwind CSS 类名，动态类名用 `clsx`。
```typescript
<div className={clsx('rounded', isActive && 'border-blue-500')}>
```
**Don't**: 写自定义 CSS 文件或使用 `style={{}}` 内联样式（除非 Tailwind 不支持的特殊场景）。
**Self-check**: 项目中没有 `.css` 文件（除 Tailwind 入口），组件中极少出现 `style={{}}`。

## Post-Coding Verification
**Do**: 编码后必执行：
```bash
npm run lint         # ESLint 检查
npx tsc --noEmit     # TypeScript 类型检查
```
**Self-check**: 每次任务完成后 lint 和类型检查无报错。
