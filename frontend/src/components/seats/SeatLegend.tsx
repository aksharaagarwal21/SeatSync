import { Lock, X } from 'lucide-react'
import { cn } from '../../lib/cn'

const ITEMS = [
  { label: 'Available', swatch: 'border-zinc-300 bg-white', icon: null },
  { label: 'Selected', swatch: 'border-brand-700 bg-brand-700', icon: null },
  { label: 'Booked', swatch: 'border-zinc-200 bg-zinc-200 text-zinc-400', icon: X },
  { label: 'Held temporarily', swatch: 'border-amber-300 bg-amber-100 text-amber-700', icon: Lock },
] as const

export function SeatLegend({ className }: { className?: string }) {
  return (
    <ul className={cn('flex flex-wrap items-center gap-x-5 gap-y-2 text-[13px] text-zinc-600', className)} aria-label="Seat legend">
      {ITEMS.map(({ label, swatch, icon: Icon }) => (
        <li key={label} className="flex items-center gap-2">
          <span className={cn('flex size-4 items-center justify-center rounded-t-[5px] rounded-b-[3px] border', swatch)} aria-hidden>
            {Icon && <Icon className="size-2.5" />}
          </span>
          {label}
        </li>
      ))}
    </ul>
  )
}
