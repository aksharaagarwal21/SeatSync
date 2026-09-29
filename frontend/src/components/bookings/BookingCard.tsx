import { useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarDays, MapPin } from 'lucide-react'
import type { Booking } from '../../lib/types'
import { formatPrice, formatShortDate, formatTime, pluralize } from '../../lib/format'
import { BookingStatusBadge } from './BookingStatusBadge'
import { Button } from '../ui/Button'
import { EventImage } from '../events/EventImage'

interface BookingCardProps {
  booking: Booking
  onCancel: (booking: Booking) => void
  cancelling: boolean
}

export function BookingCard({ booking, onCancel, cancelling }: BookingCardProps) {
  const [confirming, setConfirming] = useState(false)
  const seatNumbers = booking.seats.map((seat) => seat.seatNumber).join(', ')

  return (
    <article className="card flex flex-col gap-4 p-4 sm:flex-row sm:items-center">
      <EventImage src={booking.imageUrl} alt="" width={240} className="hidden h-20 w-28 shrink-0 rounded-lg sm:block" />

      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="text-base font-semibold">
            <Link to={`/bookings/${booking.id}`} className="rounded-sm hover:text-brand-800">
              {booking.eventName}
            </Link>
          </h2>
          <BookingStatusBadge status={booking.status} />
        </div>
        <div className="mt-1.5 flex flex-wrap gap-x-4 gap-y-1 text-[13px] text-zinc-500">
          <span className="flex items-center gap-1.5">
            <CalendarDays className="size-3.5" aria-hidden />
            {formatShortDate(booking.eventDate)} · {formatTime(booking.startTime)}
          </span>
          <span className="flex items-center gap-1.5">
            <MapPin className="size-3.5" aria-hidden />
            {booking.venue}, {booking.city}
          </span>
        </div>
        <p className="mt-2 text-sm">
          <span className="text-zinc-500">{pluralize(booking.seats.length, 'seat')}:</span> <span className="font-medium">{seatNumbers}</span>
        </p>
      </div>

      <div className="flex items-end justify-between gap-4 border-t border-zinc-100 pt-3 sm:flex-col sm:items-end sm:border-0 sm:pt-0">
        <div className="sm:text-right">
          <p className="text-base font-semibold tabular-nums">{formatPrice(booking.totalAmount)}</p>
          <p className="font-mono text-xs text-zinc-500">{booking.reference}</p>
        </div>
        {booking.cancellable &&
          (confirming ? (
            <div className="flex items-center gap-2" role="group" aria-label="Confirm cancellation">
              <Button variant="ghost" size="sm" onClick={() => setConfirming(false)} disabled={cancelling}>
                Keep
              </Button>
              <Button variant="danger" size="sm" onClick={() => onCancel(booking)} loading={cancelling}>
                Cancel booking
              </Button>
            </div>
          ) : (
            <Button variant="ghost" size="sm" onClick={() => setConfirming(true)}>
              Cancel
            </Button>
          ))}
      </div>
    </article>
  )
}
