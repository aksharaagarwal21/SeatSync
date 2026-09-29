import type { ReactNode } from 'react'

export function AuthCard({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return (
    <div className="mx-auto w-full max-w-sm pt-6 sm:pt-14">
      <div className="card p-6 sm:p-7">
        <h1 className="text-xl font-semibold tracking-tight">{title}</h1>
        <p className="mt-1 text-sm text-zinc-500">{subtitle}</p>
        {children}
      </div>
    </div>
  )
}
