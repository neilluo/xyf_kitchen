import axios, { type AxiosResponse } from 'axios'
import type { ApiResponse } from '@/types/common'

// ApiError class for API response errors
export class ApiError extends Error {
  code: number
  errors?: FieldError[]

  constructor(code: number, message: string, errors?: FieldError[]) {
    super(message)
    this.code = code
    this.errors = errors
  }
}

interface FieldError {
  field: string
  message: string
}

// ---------- EagleEye trace ----------
interface TraceMetadata {
  traceId: string
  start: number
}

declare module 'axios' {
  interface InternalAxiosRequestConfig {
    metadata?: TraceMetadata
  }
}

/**
 * 生成 30 位 EagleEye 风格 traceId。
 * 格式与后端 EagleEyeIdGenerator 对齐：ip(8)+ts(13)+seq(4)+pid(2)+ext(3)。
 * 浏览器拿不到真实 IP，用随机 hex 代替（隐私友好）。
 */
function genFrontendTraceId(): string {
  const hex = (n: number, len: number) => n.toString(16).padStart(len, '0')
  const ip = Array.from({ length: 4 }, () => hex(Math.floor(Math.random() * 256), 2)).join('')
  const ts = Date.now().toString()                              // 13
  const seq = hex(Math.floor(Math.random() * 10000), 4)         // 4
  const pid = hex(Math.floor(Math.random() * 256), 2)           // 2
  const ext = hex(Math.floor(Math.random() * 4096), 3)          // 3
  return (ip + ts + seq + pid + ext).substring(0, 30)
}

// Create axios instance with base configuration
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor: inject EagleEye trace header + record start time
apiClient.interceptors.request.use((cfg) => {
  const tid = genFrontendTraceId()
  cfg.headers['X-EagleEye-TraceId'] = tid
  cfg.headers['X-EagleEye-RpcId'] = '0'
  cfg.metadata = { traceId: tid, start: Date.now() }
  return cfg
})

// Response interceptor: unwrap ApiResponse envelope + console 打印 trace 摘要
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    const { traceId, start } = response.config.metadata ?? { traceId: '-', start: Date.now() }
    const rt = Date.now() - start
    // eslint-disable-next-line no-console
    console.debug(
      `[EagleEye] ${traceId} ${response.config.method?.toUpperCase()} ${response.config.url} ${response.status} ${rt}ms`
    )
    const data = response.data as ApiResponse<unknown>
    if (data.code !== 0) {
      return Promise.reject(new ApiError(data.code, data.message, data.errors as FieldError[]))
    }
    return response
  },
  (error) => {
    const meta = error.config?.metadata
    const traceId = meta?.traceId ?? '-'
    const rt = meta ? Date.now() - meta.start : -1
    // eslint-disable-next-line no-console
    console.error(
      `[EagleEye] ${traceId} ${error.config?.method?.toUpperCase()} ${error.config?.url} FAIL ${rt}ms`,
      error
    )
    if (error.response) {
      const data = error.response.data
      return Promise.reject(new ApiError(data?.code ?? 9999, data?.message ?? '网络错误', data?.errors as FieldError[]))
    }
    return Promise.reject(new ApiError(9999, '网络连接失败'))
  }
)

export default apiClient
