// Utility functions
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/**
 * Combines clsx and tailwind-merge for cleaner className handling
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
