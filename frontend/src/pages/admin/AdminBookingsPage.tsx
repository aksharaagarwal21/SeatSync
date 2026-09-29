import { useState } from 'react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Ticket } from 'lucide-react'
import { adminApi, queryKeys } from '../../api/queries'
import { AdminTable, Pagination, TableSkeleton } from '../../components/admin/AdminTable'
import { BookingStatusBadge } from '../../components/bookings/BookingStatusBadge'
import { EmptyState, ErrorState } from '../../components/ui/States'
import { errorMessage } from '../../lib/api'
import { formatDateTime, formatPrice, formatShortDate } from '../../lib/format'
import type { BookingStatus } from '../../lib/types'

export function AdminBookingsPage() {
  const [eventId, setEventId] = useState<number | undefined>()
  const [status, setStatus] = useState<BookingStatus | ''>('')
  const [page, setPage] = useState(0)

  const { data: events } = useQuery({ queryKey: queryKeys.adminEvents('', 0), queryFn: () => adminApi.events('', 0) })
  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: queryKeys.adminBookings(eventId, status, page),
    queryFn: () => adminApi.bookings(eventId, status, page),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <div className="mb-4 grid grid-cols-2 gap-2 sm:flex">
        <select
          className="input sm:w-72"
          aria-label="Filter by event"
          value={eventId ?? ''}
          onChange={(event) => {
            setEventId(event.target.value ? Number(event.target.value) : undefined)
            setPage(0)
          }}
        >
          <option value="">All events</option>
          {events?.content.map((event) => (
            <option key={event.id} value={event.id}>
              {event.name} · {formatShortDate(event.eventDate)}
            </option>
          ))}
        </select>
        <select
          className="input sm:w-44"
          aria-label="Filter by status"
          value={status}
          onChange={(event) => {
            setStatus(event.target.value as BookingStatus | '')
            setPage(0)
          }}
        >
          <option value="">All statuses</option>
          <option value="CONFIRMED">Confirmed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
      </div>

      {isPending ? (
        <TableSkeleton />
      ) : isError ? (
        <ErrorState message={errorMessage(error)} onRetry={() => refetch()} />
      ) : data.content.length === 0 ? (
        <EmptyState icon={Ticket} title="No bookings match these filters" />
      ) : (
        <>
          <AdminTable caption="Bookings" head={['Booking', 'Customer', 'Event', 'Seats', 'Amount', 'Status', 'Booked']}>
            {data.content.map((booking) => (
              <tr key={booking.id}>
                <td className="px-4 py-3 font-mono text-xs">{booking.reference}</td>
                <td className="px-4 py-3">
                  <p>{booking.customerName}</p>
                  <p className="text-xs text-zinc-500">{booking.customerEmail}</p>
                </td>
                <td className="max-w-56 px-4 py-3">
                  <p className="truncate">{booking.eventName}</p>
                  <p className="text-xs text-zinc-500">{formatShortDate(booking.eventDate)}</p>
                </td>
                <td className="px-4 py-3 text-zinc-600">{booking.seatNumbers.join(', ')}</td>
                <td className="px-4 py-3 tabular-nums">{formatPrice(booking.totalAmount)}</td>
                <td className="px-4 py-3">
                  <BookingStatusBadge status={booking.status} />
                </td>
                <td className="px-4 py-3 whitespace-nowrap text-zinc-500">{formatDateTime(booking.bookingTime)}</td>
              </tr>
            ))}
          </AdminTable>
          <Pagination page={data} onChange={setPage} />
        </>
      )}
    </>
  )
}
