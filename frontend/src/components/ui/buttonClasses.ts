import { cn } from '../../lib/cn'

export type Variant = 'primary' | 'secondary' | 'danger' | 'ghost'
export type Size = 'sm' | 'md' | 'lg'

const variants: Record<Variant, string> = {
  primary: 'bg-brand-700 text-white hover:bg-brand-800 disabled:bg-zinc-300 disabled:text-zinc-500',
  secondary: 'border border-zinc-300 bg-white text-zinc-800 hover:border-zinc-400 hover:bg-zinc-50 disabled:text-zinc-400',
  danger: 'border border-red-200 bg-white text-red-700 hover:border-red-300 hover:bg-red-50 disabled:text-red-300',
  ghost: 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900 disabled:text-zinc-400',
}

const sizes: Record<Size, string> = {
  sm: 'h-8 gap-1.5 px-3 text-[13px]',
  md: 'h-10 gap-2 px-4 text-sm',
  lg: 'h-11 gap-2 px-5 text-[15px]',
}

export function buttonClasses(variant: Variant = 'primary', size: Size = 'md', className?: string) {
  return cn(
    'inline-flex shrink-0 items-center justify-center rounded-lg font-medium transition-colors disabled:cursor-not-allowed',
    variants[variant],
    sizes[size],
    className,
  )
}
