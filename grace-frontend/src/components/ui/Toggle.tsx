import { useId } from 'react'

interface ToggleProps {
  checked: boolean
  onChange: (checked: boolean) => void
  disabled?: boolean
  label?: string
  className?: string
}

export function Toggle({ checked, onChange, disabled = false, label, className = '' }: ToggleProps) {
  const id = useId()

  return (
    <div className={`flex items-center gap-3 ${className}`}>
      <div className="relative inline-flex">
        <input
          id={id}
          type="checkbox"
          checked={checked}
          onChange={(e) => onChange(e.target.checked)}
          disabled={disabled}
          className="peer sr-only"
        />
        <label
          htmlFor={id}
          className={`relative w-11 h-6 bg-surface-container-highest rounded-full cursor-pointer transition-colors peer-checked:bg-primary peer-disabled:opacity-40 peer-disabled:cursor-not-allowed ${
            disabled ? 'cursor-not-allowed opacity-40' : ''
          }`}
        >
          <span
            className={`absolute top-0.5 left-[2px] bg-white rounded-full h-5 w-5 transition-all ${
              checked ? 'translate-x-full' : ''
            }`}
          />
        </label>
      </div>
      {label && (
        <label htmlFor={id} className="text-sm font-body text-on-surface cursor-pointer">
          {label}
        </label>
      )}
    </div>
  )
}
