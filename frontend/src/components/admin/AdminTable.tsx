import type { ReactNode } from 'react'
import { Button } from '../ui/Button'
import { Skeleton } from '../ui/Skeleton'
import type { Page } from '../../lib/types'

/** A horizontally scrollable table so wide admin data stays usable on phones. */
export function AdminTable({ head, children, caption }: { head: string[]; children: ReactNode; caption: string }) {
  return (
    <div className="card overflow-x-auto">
      <table className="w-full min-w-[640px] text-left text-sm">
        <caption className="sr-only">{caption}</caption>
        <thead>
          <tr className="border-b border-zinc-200 text-xs font-medium text-zinc-500">
            {head.map((label) => (
              <th key={label} scope="col" className="px-4 py-3 font-medium whitespace-nowrap">
                {label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-zinc-100">{children}</tbody>
      </table>
    </div>
  )
}

export function TableSkeleton({ rows = 6 }: { rows?: number }) {
  return (
    <div className="card space-y-3 p-4" aria-busy>
      {Array.from({ length: rows }, (_, index) => (
        <Skeleton key={index} className="h-6 w-full" />
      ))}
    </div>
  )
}

export function Pagination({ page, onChange }: { page: Page<unknown>; onChange: (page: number) => void }) {
  if (page.totalPages <= 1) return null
  return (
    <div className="mt-4 flex items-center justify-between text-sm text-zinc-500">
      <span>
        Page {page.page + 1} of {page.totalPages} · {page.totalElements} total
      </span>
      <div className="flex gap-2">
        <Button variant="secondary" size="sm" disabled={page.page === 0} onClick={() => onChange(page.page - 1)}>
          Previous
        </Button>
        <Button variant="secondary" size="sm" disabled={page.last} onClick={() => onChange(page.page + 1)}>
          Next
        </Button>
      </div>
    </div>
  )
}
