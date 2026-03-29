interface IconProps {
  name: string
  className?: string
  size?: number
  style?: React.CSSProperties
}

export function Icon({ name, className = '', size = 20, style }: IconProps) {
  return (
    <span
      className={`material-symbols-outlined ${className}`}
      style={{ fontSize: size, ...style }}
    >
      {name}
    </span>
  )
}
