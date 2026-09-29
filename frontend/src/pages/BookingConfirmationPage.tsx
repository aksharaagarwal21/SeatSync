import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { CheckCircle2, XCircle } from 'lucide-react'
import { bookingApi, queryKeys } from '../api/queries'
import { ButtonLink } from '../components/ui/Button'
import { Skeleton } from '../components/ui/Skeleton'
import { ErrorState } from '../components/ui/States'
import { errorMessage } from '../lib/api'
import { BOOKING_STATUS_LABELS, formatLongDate, formatPrice, formatTime } from '../lib/format'
import type { Booking } from '../lib/types'

export function BookingConfirmationPage() {
  const bookingId = Number(useParams().bookingId)
  const { data: booking, isPending, isError, error, refetch } = useQuery({
    queryKey: queryKeys.booking(bookingId),
    queryFn: () => bookingApi.get(bookingId),
  })

  if (isPending) {
    return (
      <div className="mx-auto max-w-md space-y-4 pt-10" aria-busy>
        <Skeleton className="mx-auto size-12 rounded-full" />
        <Skeleton className="mx-auto h-7 w-52" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>
    )
  }
  if (isError) return <ErrorState message={errorMessage(error, 'We couldn’t load this booking.')} onRetry={() => refetch()} />

  const cancelled = booking.status === 'CANCELLED'

  return (
    <div className="mx-auto max-w-md pt-4 sm:pt-10">
      <div className="text-center">
        <div className={`mx-auto flex size-14 items-center justify-center rounded-full ${cancelled ? 'bg-zinc-100' : 'bg-brand-50'}`}>
          {cancelled ? <XCircle className="size-7 text-zinc-500" aria-hidden /> : <CheckCircle2 className="size-7 text-brand-600" aria-hidden />}
        </div>
        <h1 className="mt-4 text-[28px] leading-tight font-semibold tracking-tight">
          {cancelled ? 'Booking Cancelled' : `Booking ${BOOKING_STATUS_LABELS[booking.status]}`}
        </h1>
        {!cancelled && <p className="mt-1 text-sm text-zinc-500">Your seats are reserved. Show your booking ID at the venue.</p>}
      </div>

      <BookingDetails booking={booking} />

      <div className="mt-6 flex flex-col gap-2 sm:flex-row">
        <ButtonLink to="/bookings" className="flex-1">
          View My Bookings
        </ButtonLink>
        <ButtonLink to="/" variant="secondary" className="flex-1">
          Back to Events
        </ButtonLink>
      </div>
    </div>
  )
}

function BookingDetails({ booking }: { booking: Booking }) {
  const rows: Array<[string, string]> = [
    ['Event', booking.eventName],
    ['Date', `${formatLongDate(booking.eventDate)} · ${formatTime(booking.startTime)}`],
    ['Venue', `${booking.venue}, ${booking.city}`],
    ['Seats', booking.seats.map((seat) => seat.seatNumber).join(', ')],
    ['Amount', formatPrice(booking.totalAmount)],
  ]
  return (
    <dl className="card mt-6 divide-y divide-zinc-100 text-sm">
      {rows.map(([label, value]) => (
        <div key={label} className="flex justify-between gap-6 px-4 py-3">
          <dt className="text-zinc-500">{label}</dt>
          <dd className="text-right font-medium">{value}</dd>
        </div>
      ))}
      <div className="flex justify-between gap-6 px-4 py-3">
        <dt className="text-zinc-500">Booking ID</dt>
        <dd className="font-mono text-[13px] font-semibold tracking-wide">{booking.reference}</dd>
      </div>
    </dl>
  )
}
