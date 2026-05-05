# 前端 axios 模板

前端只做一件事：每次请求生成 30 位 EagleEye traceId 并通过 header 注入；响应回来时在 console 打印便于排查。

**仅改 1 个文件**：axios 实例所在模块（通常是 `src/services/*.js` 或 `src/api/request.js`）。

---

## 完整 interceptor 代码

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 30_000,
});

/**
 * 生成 30 位 EagleEye 风格 traceId。
 * 前端 IP 段用随机 hex 代替（隐私友好），其余与后端格式对齐。
 */
function genFrontendTraceId() {
  const hex = (n, len) => n.toString(16).padStart(len, '0');
  const ip  = Array.from({ length: 4 }, () => hex(Math.floor(Math.random() * 256), 2)).join('');
  const ts  = Date.now().toString();                              // 13 位
  const seq = hex(Math.floor(Math.random() * 10000), 4);          // 4 位
  const pid = hex(Math.floor(Math.random() * 256), 2);            // 2 位
  const ext = hex(Math.floor(Math.random() * 4096), 3);           // 3 位
  return (ip + ts + seq + pid + ext).substring(0, 30);
}

// 请求拦截器：注入 trace header + 记录开始时间
api.interceptors.request.use(cfg => {
  const tid = genFrontendTraceId();
  cfg.headers['X-EagleEye-TraceId'] = tid;
  cfg.headers['X-EagleEye-RpcId'] = '0';
  cfg.metadata = { traceId: tid, start: Date.now() };
  return cfg;
});

// 响应拦截器：console 打印摘要，便于与后端 traceId 对齐
api.interceptors.response.use(
  res => {
    const { traceId, start } = res.config.metadata || {};
    const rt = Date.now() - (start || Date.now());
    console.debug(
      `[EagleEye] ${traceId} ${res.config.method?.toUpperCase()} ${res.config.url} ${res.status} ${rt}ms`
    );
    return res;
  },
  err => {
    const { traceId, start } = err.config?.metadata || {};
    const rt = Date.now() - (start || Date.now());
    console.error(
      `[EagleEye] ${traceId} ${err.config?.method?.toUpperCase()} ${err.config?.url} FAIL ${rt}ms`,
      err
    );
    return Promise.reject(err);
  }
);

export default api;
```

---

## 验证方式

1. 打开浏览器 DevTools → Network 面板
2. 任意发起一次 API 请求
3. 检查 Request Headers：应含 `X-EagleEye-TraceId: <30 位 hex>`、`X-EagleEye-RpcId: 0`
4. 检查 Response Headers：后端回写的 `X-EagleEye-TraceId` 应与请求相同
5. Console 面板应打印 `[EagleEye] <traceId> <METHOD> <URL> <status> <rt>ms`
6. 复制该 traceId，后端 `grep <traceId> ~/logs/{appName}/*.log` 必须能命中

---

## 关键点

- **traceId 必须前端生成**：由前端发起才能把"用户点击"那一刻与后端日志串起来
- **RpcId 固定传 `0`**：rpcId 的层级递增由后端 `TraceContext.pushSpan` 管理，前端不参与
- **console 打印不依赖生产环境**：`console.debug` 默认在 Verbose 级别可见，生产排查时可开
- **不要用 uuid/nanoid**：长度/格式不对齐会导致日志中一眼看不出来源；固定 30 位 hex 与后端格式一致
- **IP 段随机化**：浏览器拿不到真实外网 IP，也不该泄露本机内网 IP；随机 hex 只保证字符集合法

---

## TypeScript 版本

```ts
interface TraceMetadata {
  traceId: string;
  start: number;
}

declare module 'axios' {
  interface InternalAxiosRequestConfig {
    metadata?: TraceMetadata;
  }
}

function genFrontendTraceId(): string {
  const hex = (n: number, len: number) => n.toString(16).padStart(len, '0');
  const ip  = Array.from({ length: 4 }, () => hex(Math.floor(Math.random() * 256), 2)).join('');
  const ts  = Date.now().toString();
  const seq = hex(Math.floor(Math.random() * 10000), 4);
  const pid = hex(Math.floor(Math.random() * 256), 2);
  const ext = hex(Math.floor(Math.random() * 4096), 3);
  return (ip + ts + seq + pid + ext).substring(0, 30);
}
```

其余 interceptor 逻辑与 JS 版一致。
