import type { ReactNode } from 'react'
import type { LucideIcon } from 'lucide-react'
import { AlertCircle } from 'lucide-react'
import { Button } from './Button'

interface EmptyStateProps {
  icon: LucideIcon
  title: string
  description?: string
  action?: ReactNode
}

export function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center rounded-xl border border-dashed border-zinc-300 bg-white px-6 py-14 text-center">
      <div className="mb-4 flex size-11 items-center justify-center rounded-full bg-zinc-100">
        <Icon className="size-5 text-zinc-500" aria-hidden />
      </div>
      <h2 className="text-base font-semibold text-zinc-900">{title}</h2>
      {description && <p className="mt-1 max-w-sm text-sm text-zinc-500">{description}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div role="alert" className="flex flex-col items-center rounded-xl border border-red-100 bg-white px-6 py-12 text-center">
      <AlertCircle className="mb-3 size-6 text-red-500" aria-hidden />
      <p className="max-w-md text-sm text-zinc-700">{message}</p>
      {onRetry && (
        <Button variant="secondary" size="sm" className="mt-4" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  )
}
