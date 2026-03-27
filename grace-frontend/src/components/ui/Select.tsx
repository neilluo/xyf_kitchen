// Select - 下拉选择组件
// 遵循设计系统 §5.5：与 Input 相同基底样式，附加下拉箭头图标

import { cn } from '@/lib/utils'
import { Icon } from './Icon'

interface SelectOption {
  value: string
  label: string
}

interface SelectProps extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'onChange'> {
  className?: string
  options: SelectOption[]
  placeholder?: string
  error?: boolean
  onChange?: (value: string) => void
}

export function Select({
  className,
  options,
  placeholder,
  error,
  onChange,
  ...props
}: SelectProps) {
  return (
    <div className="relative">
      <select
        className={cn(
          // 基础样式（与 Input 相同）
          'w-full bg-surface-container-low rounded-md px-4 py-2.5',
          'text-sm font-body text-on-surface',
          // 无 border
          'border-0 appearance-none',
          // 右侧留出箭头空间
          'pr-10',
          // Focus 状态
          'focus:outline-none focus:ring-2 focus:ring-primary/40',
          // Error 状态
          error && 'bg-error-container/10 focus:ring-error/40',
          // Disabled 状态
          'disabled:opacity-50 disabled:cursor-not-allowed',
          className
        )}
        onChange={(e) => onChange?.(e.target.value)}
        {...props}
      >
        {placeholder && (
          <option value="" disabled>
            {placeholder}
          </option>
        )}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {/* 下拉箭头图标 */}
      <div className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-on-surface-variant">
        <Icon name="expand_more" size={18} />
      </div>
    </div>
  )
}

interface NativeSelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  className?: string
  error?: boolean
  children: React.ReactNode
}

export function NativeSelect({ className, error, children, ...props }: NativeSelectProps) {
  return (
    <div className="relative">
      <select
        className={cn(
          // 基础样式
          'w-full bg-surface-container-low rounded-md px-4 py-2.5',
          'text-sm font-body text-on-surface',
          // 无 border
          'border-0 appearance-none',
          // 右侧留出箭头空间
          'pr-10',
          // Focus 状态
          'focus:outline-none focus:ring-2 focus:ring-primary/40',
          // Error 状态
          error && 'bg-error-container/10 focus:ring-error/40',
          // Disabled 状态
          'disabled:opacity-50 disabled:cursor-not-allowed',
          className
        )}
        {...props}
      >
        {children}
      </select>
      {/* 下拉箭头图标 */}
      <div className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-on-surface-variant">
        <Icon name="expand_more" size={18} />
      </div>
    </div>
  )
}
