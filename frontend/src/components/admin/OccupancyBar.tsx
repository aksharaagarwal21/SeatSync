export function OccupancyBar({ used, total }: { used: number; total: number }) {
  const ratio = total ? used / total : 0
  return (
    <div className="flex items-center gap-2.5">
      <div
        className="h-1.5 w-24 overflow-hidden rounded-full bg-zinc-100"
        role="meter"
        aria-valuemin={0}
        aria-valuemax={total}
        aria-valuenow={used}
        aria-label="Seats sold"
      >
        <div className="h-full rounded-full bg-brand-600" style={{ width: `${Math.min(ratio, 1) * 100}%` }} />
      </div>
      <span className="text-xs whitespace-nowrap text-zinc-600 tabular-nums">
        {used}/{total}
      </span>
    </div>
  )
}
