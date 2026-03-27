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

// Create axios instance with base configuration
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Response interceptor: unwrap ApiResponse envelope
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    const data = response.data as ApiResponse<unknown>
    if (data.code !== 0) {
      return Promise.reject(new ApiError(data.code, data.message, data.errors as FieldError[]))
    }
    return response
  },
  (error) => {
    if (error.response) {
      const data = error.response.data
      return Promise.reject(new ApiError(data?.code ?? 9999, data?.message ?? '网络错误', data?.errors as FieldError[]))
    }
    return Promise.reject(new ApiError(9999, '网络连接失败'))
  }
)

export default apiClient
