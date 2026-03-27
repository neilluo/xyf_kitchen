// 统一响应信封
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

// 分页响应
export interface PaginatedData<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

// 分页查询参数
export interface PaginationParams {
  page?: number
  pageSize?: number
  sort?: string
  order?: 'asc' | 'desc'
}

// 字段错误
export interface FieldError {
  field: string
  message: string
}
