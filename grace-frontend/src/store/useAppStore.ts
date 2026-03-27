import { create } from 'zustand'

interface Toast {
  id: string
  type: 'success' | 'error' | 'info'
  message: string
}

interface UploadQueueItem {
  file: File
  uploadId: string | null
  progress: number
  status: 'pending' | 'uploading' | 'completed' | 'failed'
}

interface AppStore {
  // Toast 通知
  toasts: Toast[]
  addToast: (toast: Omit<Toast, 'id'>) => void
  removeToast: (id: string) => void

  // 上传队列
  uploadQueue: UploadQueueItem[]
  addToUploadQueue: (file: File) => void
  updateUploadItem: (index: number, updates: Partial<UploadQueueItem>) => void
  removeFromUploadQueue: (index: number) => void
}

export const useAppStore = create<AppStore>((set) => ({
  // Toast 通知初始状态
  toasts: [],

  // 添加 Toast - 自动生成唯一 ID
  addToast: (toast) =>
    set((state) => ({
      toasts: [
        ...state.toasts,
        {
          ...toast,
          id: `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        },
      ],
    })),

  // 移除指定 ID 的 Toast
  removeToast: (id) =>
    set((state) => ({
      toasts: state.toasts.filter((toast) => toast.id !== id),
    })),

  // 上传队列初始状态
  uploadQueue: [],

  // 添加文件到上传队列
  addToUploadQueue: (file) =>
    set((state) => ({
      uploadQueue: [
        ...state.uploadQueue,
        {
          file,
          uploadId: null,
          progress: 0,
          status: 'pending',
        },
      ],
    })),

  // 更新上传队列项
  updateUploadItem: (index, updates) =>
    set((state) => {
      if (index < 0 || index >= state.uploadQueue.length) {
        return state
      }
      const newQueue = [...state.uploadQueue]
      newQueue[index] = { ...newQueue[index], ...updates }
      return { uploadQueue: newQueue }
    }),

  // 从上传队列移除
  removeFromUploadQueue: (index) =>
    set((state) => {
      if (index < 0 || index >= state.uploadQueue.length) {
        return state
      }
      const newQueue = [...state.uploadQueue]
      newQueue.splice(index, 1)
      return { uploadQueue: newQueue }
    }),
}))
