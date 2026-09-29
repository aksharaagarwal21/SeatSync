import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { adminApi, queryKeys, useSeats } from '../../api/queries'
import { OccupancyBar } from '../../components/admin/OccupancyBar'
import { TableSkeleton } from '../../components/admin/AdminTable'
import { SeatLegend } from '../../components/seats/SeatLegend'
import { SeatMap } from '../../components/seats/SeatMap'
import { Skeleton } from '../../components/ui/Skeleton'
import { ErrorState } from '../../components/ui/States'
import { useSeatUpdates } from '../../hooks/useSeatUpdates'
import { errorMessage } from '../../lib/api'
import { cn } from '../../lib/cn'
import { formatShortDate } from '../../lib/format'
import type { AdminEvent } from '../../lib/types'

const NO_SELECTION = new Set<number>()

/** Live seat monitoring: occupancy per upcoming event, and the seat map of the selected one. */
export function AdminSeatsPage() {
  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: queryKeys.adminEvents('', 0),
    queryFn: () => adminApi.events('', 0),
    refetchInterval: 30_000,
  })
  const [selectedId, setSelectedId] = useState<number | null>(null)

  if (isPending) return <TableSkeleton />
  if (isError) return <ErrorState message={errorMessage(error)} onRetry={() => refetch()} />

  const selected = data.content.find((event) => event.id === selectedId) ?? data.content[0]

  return (
    <div className="grid gap-5 lg:grid-cols-[320px_minmax(0,1fr)]">
      <ul className="card max-h-[640px] divide-y divide-zinc-100 overflow-y-auto" aria-label="Upcoming events">
        {data.content.map((event) => (
          <li key={event.id}>
            <button
              type="button"
              onClick={() => setSelectedId(event.id)}
              aria-current={event.id === selected?.id ? 'true' : undefined}
              className={cn(
                'w-full px-4 py-3 text-left transition-colors hover:bg-zinc-50',
                event.id === selected?.id && 'bg-brand-50/60 hover:bg-brand-50/60',
              )}
            >
              <p className="truncate text-sm font-medium">{event.name}</p>
              <p className="mb-2 text-xs text-zinc-500">
                {formatShortDate(event.eventDate)} · {event.city}
              </p>
              <OccupancyBar used={event.bookedSeats + event.heldSeats} total={event.totalSeats} />
            </button>
          </li>
        ))}
      </ul>
      {selected && <EventSeatMonitor key={selected.id} event={selected} />}
    </div>
  )
}

function EventSeatMonitor({ event }: { event: AdminEvent }) {
  const { data: seats, isPending } = useSeats(event.id)
  useSeatUpdates(event.id)
  const counts = {
    available: seats?.filter((seat) => seat.status === 'AVAILABLE').length ?? event.availableSeats,
    held: seats?.filter((seat) => seat.status === 'RESERVED').length ?? event.heldSeats,
    booked: seats?.filter((seat) => seat.status === 'BOOKED').length ?? event.bookedSeats,
  }

  return (
    <section className="card min-w-0 p-5" aria-labelledby="seat-monitor-heading">
      <h2 id="seat-monitor-heading" className="text-base font-semibold">
        {event.name}
      </h2>
      <dl className="mt-3 grid grid-cols-3 gap-3 text-sm">
        {(
          [
            ['Available', counts.available],
            ['Held', counts.held],
            ['Booked', counts.booked],
          ] as const
        ).map(([label, value]) => (
          <div key={label} className="rounded-lg bg-zinc-50 px-3 py-2">
            <dt className="text-xs text-zinc-500">{label}</dt>
            <dd className="text-lg font-semibold tabular-nums">{value}</dd>
          </div>
        ))}
      </dl>
      <SeatLegend className="my-5 justify-center" />
      {isPending ? <Skeleton className="h-80 w-full" /> : <SeatMap seats={seats ?? []} selectedIds={NO_SELECTION} readOnly />}
    </section>
  )
}
