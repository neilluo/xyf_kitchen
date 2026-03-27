interface ProgressBarProps {
  progress: number
  className?: string
}

export function ProgressBar({ progress, className = '' }: ProgressBarProps) {
  const clampedProgress = Math.max(0, Math.min(100, progress))

  return (
    <div className={`w-full h-2 bg-surface-container-highest rounded-full ${className}`}>
      <div
        className="h-2 bg-primary rounded-full transition-all duration-300"
        style={{ width: `${clampedProgress}%` }}
        role="progressbar"
        aria-valuenow={clampedProgress}
        aria-valuemin={0}
        aria-valuemax={100}
      />
    </div>
  )
}
