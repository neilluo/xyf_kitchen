import { ReactNode, ButtonHTMLAttributes } from 'react';
import { Icon } from './Icon';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'icon';

interface ButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'onClick'> {
  variant?: ButtonVariant;
  children?: ReactNode;
  icon?: string;
  disabled?: boolean;
  onClick?: () => void;
  className?: string;
}

const variantClasses: Record<ButtonVariant, string> = {
  primary: 'bg-gradient-to-r from-primary to-primary-container text-white rounded-lg px-6 py-2.5 font-body text-sm font-medium hover:opacity-90 transition-opacity',
  secondary: 'bg-surface-container-high text-on-surface rounded-lg px-4 py-2 font-body text-sm hover:bg-surface-container transition-colors',
  ghost: 'text-on-surface-variant hover:bg-surface-container-low rounded-lg px-3 py-2 font-body text-sm transition-colors',
  danger: 'bg-error text-white rounded-lg px-4 py-2 font-body text-sm hover:bg-error/90 transition-colors',
  icon: 'p-2 rounded-full hover:bg-primary/10 text-on-surface-variant transition-colors',
};

export function Button({
  variant = 'primary',
  children,
  icon,
  disabled = false,
  onClick,
  className = '',
  ...rest
}: ButtonProps) {
  const baseClasses = variantClasses[variant];
  const disabledClasses = disabled ? 'opacity-50 cursor-not-allowed' : '';

  return (
    <button
      type="button"
      className={`${baseClasses} ${disabledClasses} ${className}`}
      disabled={disabled}
      onClick={onClick}
      {...rest}
    >
      {icon && variant === 'icon' ? (
        <Icon name={icon} size={20} />
      ) : (
        <>
          {icon && <Icon name={icon} size={18} className="mr-1.5" />}
          {children}
        </>
      )}
    </button>
  );
}
