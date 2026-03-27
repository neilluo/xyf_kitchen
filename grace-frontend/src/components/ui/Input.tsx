// Input - 输入框组件
// 遵循设计系统 §5.4：无 border，focus 时 ring-2 ring-primary/40

import { cn } from '@/lib/utils'

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  className?: string
  error?: boolean
}

export function Input({ className, error, ...props }: InputProps) {
  return (
    <input
      className={cn(
        // 基础样式
        'w-full bg-surface-container-low rounded-md px-4 py-2.5',
        'text-sm font-body text-on-surface',
        'placeholder:text-on-surface-variant/50',
        // 无 border（遵循 No-Line 规则）
        'border-0',
        // Focus 状态
        'focus:outline-none focus:ring-2 focus:ring-primary/40',
        // Error 状态
        error && 'bg-error-container/10 focus:ring-error/40',
        // Disabled 状态
        'disabled:opacity-50 disabled:cursor-not-allowed',
        className
      )}
      {...props}
    />
  )
}

interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  className?: string
  error?: boolean
  rows?: number
}

export function Textarea({ className, error, rows = 3, ...props }: TextareaProps) {
  return (
    <textarea
      rows={rows}
      className={cn(
        // 基础样式
        'w-full bg-surface-container-low rounded-md px-4 py-2.5',
        'text-sm font-body text-on-surface',
        'placeholder:text-on-surface-variant/50',
        // 无 border
        'border-0 resize-none',
        // Focus 状态
        'focus:outline-none focus:ring-2 focus:ring-primary/40',
        // Error 状态
        error && 'bg-error-container/10 focus:ring-error/40',
        // Disabled 状态
        'disabled:opacity-50 disabled:cursor-not-allowed',
        className
      )}
      {...props}
    />
  )
}
