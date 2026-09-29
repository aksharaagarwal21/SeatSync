import { useState } from 'react'
import { Link } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Search } from 'lucide-react'
import { adminApi, queryKeys } from '../../api/queries'
import { AdminTable, Pagination, TableSkeleton } from '../../components/admin/AdminTable'
import { OccupancyBar } from '../../components/admin/OccupancyBar'
import { Badge } from '../../components/ui/Badge'
import { Button, ButtonLink } from '../../components/ui/Button'
import { EmptyState, ErrorState } from '../../components/ui/States'
import { useToast } from '../../context/ToastContext'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { errorMessage } from '../../lib/api'
import { formatShortDate, formatTime } from '../../lib/format'
import type { AdminEvent } from '../../lib/types'

export function AdminEventsPage() {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const query = useDebouncedValue(search.trim(), 300)
  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: queryKeys.adminEvents(query, page),
    queryFn: () => adminApi.events(query, page),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative sm:w-72">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-zinc-400" aria-hidden />
          <input
            type="search"
            className="input pl-9"
            placeholder="Search upcoming events"
            aria-label="Search upcoming events"
            value={search}
            onChange={(event) => {
              setSearch(event.target.value)
              setPage(0)
            }}
          />
        </div>
        <ButtonLink to="/admin/events/new">
          <Plus className="size-4" aria-hidden /> New event
        </ButtonLink>
      </div>

      {isPending ? (
        <TableSkeleton />
      ) : isError ? (
        <ErrorState message={errorMessage(error)} onRetry={() => refetch()} />
      ) : data.content.length === 0 ? (
        <EmptyState icon={Search} title="No upcoming events found" />
      ) : (
        <>
          <AdminTable caption="Upcoming events" head={['Event', 'Date', 'Venue', 'Status', 'Seats sold', '']}>
            {data.content.map((event) => (
              <EventRow key={event.id} event={event} />
            ))}
          </AdminTable>
          <Pagination page={data} onChange={setPage} />
        </>
      )}
    </>
  )
}

function EventRow({ event }: { event: AdminEvent }) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [confirmDelete, setConfirmDelete] = useState(false)
  const remove = useMutation({
    mutationFn: () => adminApi.deleteEvent(event.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin'] })
      queryClient.invalidateQueries({ queryKey: ['events'] })
      toast.success(`${event.name} was deleted.`)
    },
    onError: (err) => {
      setConfirmDelete(false)
      toast.warning(errorMessage(err))
    },
  })
  const sold = event.bookedSeats + event.heldSeats

  return (
    <tr>
      <td className="px-4 py-3 font-medium">
        <Link to={`/events/${event.id}`} className="rounded-sm hover:text-brand-800">
          {event.name}
        </Link>
      </td>
      <td className="px-4 py-3 whitespace-nowrap text-zinc-600">
        {formatShortDate(event.eventDate)} · {formatTime(event.startTime)}
      </td>
      <td className="max-w-48 truncate px-4 py-3 text-zinc-600">
        {event.venue}, {event.city}
      </td>
      <td className="px-4 py-3">
        {event.status === 'ON_SALE' ? <Badge tone="success">On sale</Badge> : <Badge tone="muted">Paused</Badge>}
      </td>
      <td className="px-4 py-3">
        <OccupancyBar used={sold} total={event.totalSeats} />
      </td>
      <td className="px-4 py-3 text-right whitespace-nowrap">
        {confirmDelete ? (
          <span className="inline-flex gap-1">
            <Button variant="ghost" size="sm" onClick={() => setConfirmDelete(false)}>
              Keep
            </Button>
            <Button variant="danger" size="sm" loading={remove.isPending} onClick={() => remove.mutate()}>
              Delete
            </Button>
          </span>
        ) : (
          <span className="inline-flex gap-1">
            <ButtonLink to={`/admin/events/${event.id}/edit`} variant="ghost" size="sm">
              Edit
            </ButtonLink>
            <Button variant="ghost" size="sm" onClick={() => setConfirmDelete(true)} aria-label={`Delete ${event.name}`}>
              Delete
            </Button>
          </span>
        )}
      </td>
    </tr>
  )
}
