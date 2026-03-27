import { Icon } from './Icon'

interface TagChipProps {
  label: string
  onRemove?: () => void
  className?: string
}

export function TagChip({ label, onRemove, className = '' }: TagChipProps) {
  return (
    <span
      className={`inline-flex items-center gap-1 bg-tertiary-fixed text-on-tertiary-fixed-variant px-3 py-1 rounded-full text-xs font-medium ${className}`}
    >
      {label}
      {onRemove && (
        <button
          type="button"
          onClick={onRemove}
          className="inline-flex items-center justify-center hover:bg-tertiary/20 rounded-full p-0.5 -mr-1 transition-colors"
          aria-label={`Remove ${label}`}
        >
          <Icon name="close" size={14} />
        </button>
      )}
    </span>
  )
}
