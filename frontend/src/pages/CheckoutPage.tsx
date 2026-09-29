import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CalendarDays, MapPin, Timer } from 'lucide-react'
import { bookingApi, queryKeys, useEvent, useSeats, verificationApi } from '../api/queries'
import { VerificationDialog } from '../components/verification/VerificationDialog'
import { EventImage } from '../components/events/EventImage'
import { Button, ButtonLink } from '../components/ui/Button'
import { Skeleton } from '../components/ui/Skeleton'
import { EmptyState, ErrorState } from '../components/ui/States'
import { useToast } from '../context/ToastContext'
import { formatCountdown, useCountdown } from '../hooks/useCountdown'
import { useSeatUpdates } from '../hooks/useSeatUpdates'
import { ApiError, errorMessage } from '../lib/api'
import { formatLongDate, formatPrice, formatTime, SECTION_LABELS } from '../lib/format'
import { cn } from '../lib/cn'
import type { VerificationChallenge, VerificationCode } from '../lib/types'

export function CheckoutPage() {
  const eventId = Number(useParams().eventId)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const { data: event, isError: eventError, error: eventErr } = useEvent(eventId)
  const { data: seats, isPending, isError, error, refetch } = useSeats(eventId)
  useSeatUpdates(eventId)

  const heldSeats = (seats ?? []).filter((seat) => seat.heldByMe)
  const expiresAt = heldSeats.map((seat) => seat.holdExpiresAt).filter(Boolean).sort()[0] ?? null
  const secondsLeft = useCountdown(expiresAt)
  const total = heldSeats.reduce((sum, seat) => sum + seat.price, 0)

  const [challenge, setChallenge] = useState<VerificationChallenge | null>(null)
  const seatIds = heldSeats.map((seat) => seat.id)

  const book = useMutation({
    mutationFn: (verification?: VerificationCode) => bookingApi.create(eventId, seatIds, verification),
    onSuccess: (booking) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.myBookings })
      queryClient.setQueryData(queryKeys.booking(booking.id), booking)
      navigate(`/bookings/${booking.id}`, { replace: true, state: { justBooked: true } })
    },
    onError: (err) => {
      if (err instanceof ApiError && err.isConflict) {
        setChallenge(null)
        toast.warning(`${err.message} Please choose another seat.`)
        navigate(`/events/${eventId}/seats`)
      } else if (!challenge) {
        toast.warning(errorMessage(err))
      }
    },
  })

  // Step-up verification: the emailed code approves exactly these seats.
  const requestCode = useMutation({
    mutationFn: () => verificationApi.request({ purpose: 'BOOKING', eventId, seatIds }),
    onSuccess: (issued) => (issued.required ? setChallenge(issued) : book.mutate(undefined)),
    onError: (err) => toast.warning(errorMessage(err)),
  })

  if (eventError || isError) {
    return <ErrorState message={errorMessage(eventError ? eventErr : error, 'We couldn’t load your booking.')} onRetry={() => refetch()} />
  }
  if (isPending || !event) return <CheckoutSkeleton />

  const expired = heldSeats.length === 0 || secondsLeft === 0
  if (expired && !book.isPending) {
    return (
      <div className="mx-auto max-w-lg pt-8">
        <EmptyState
          icon={Timer}
          title="Your seat hold has expired"
          description="Held seats are released after 5 minutes so others can book them. Your seats may still be available."
          action={<ButtonLink to={`/events/${eventId}/seats`}>Choose seats again</ButtonLink>}
        />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-2xl">
      <Link to={`/events/${eventId}/seats`} className="mb-4 inline-flex items-center gap-1.5 rounded-md text-sm text-zinc-500 hover:text-zinc-900">
        <ArrowLeft className="size-4" aria-hidden /> Change seats
      </Link>
      <h1 className="text-[28px] leading-tight font-semibold tracking-tight">Review your booking</h1>

      <div
        className={cn(
          'mt-4 flex items-center gap-2 rounded-lg px-3.5 py-2.5 text-sm',
          secondsLeft !== null && secondsLeft < 60 ? 'bg-amber-50 text-amber-800' : 'bg-zinc-100 text-zinc-700',
        )}
        role="timer"
        aria-live="off"
      >
        <Timer className="size-4 shrink-0" aria-hidden />
        Your seats are held for <span className="font-semibold tabular-nums">{formatCountdown(secondsLeft ?? 0)}</span>
      </div>

      <div className="card mt-5 overflow-hidden">
        <div className="flex gap-4 border-b border-zinc-100 p-4 sm:p-5">
          <EventImage src={event.imageUrl} alt="" category={event.category} width={240} className="size-20 shrink-0 rounded-lg sm:h-20 sm:w-28" />
          <div className="min-w-0">
            <h2 className="font-semibold">{event.name}</h2>
            <p className="mt-1 flex items-center gap-1.5 text-[13px] text-zinc-500">
              <CalendarDays className="size-3.5" aria-hidden />
              {formatLongDate(event.eventDate)} · {formatTime(event.startTime)}
            </p>
            <p className="mt-0.5 flex items-center gap-1.5 text-[13px] text-zinc-500">
              <MapPin className="size-3.5" aria-hidden />
              {event.venue}, {event.city}
            </p>
          </div>
        </div>

        <ul className="divide-y divide-zinc-100 px-4 sm:px-5">
          {heldSeats.map((seat) => (
            <li key={seat.id} className="flex items-center justify-between py-3 text-sm">
              <span>
                <span className="font-medium">Seat {seat.seatNumber}</span>
                <span className="text-zinc-500"> · {SECTION_LABELS[seat.section]}</span>
              </span>
              <span className="tabular-nums">{formatPrice(seat.price)}</span>
            </li>
          ))}
        </ul>

        <div className="flex items-baseline justify-between border-t border-zinc-200 bg-zinc-50/60 px-4 py-4 sm:px-5">
          <span className="text-sm font-medium">Total</span>
          <span className="text-xl font-semibold tabular-nums">{formatPrice(total)}</span>
        </div>
      </div>

      <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <ButtonLink to={`/events/${eventId}/seats`} variant="secondary" size="lg">
          Change seats
        </ButtonLink>
        <Button size="lg" onClick={() => requestCode.mutate()} loading={requestCode.isPending || (book.isPending && !challenge)}>
          Confirm Booking
        </Button>
      </div>

      {challenge && (
        <VerificationDialog
          title="Confirm your booking"
          challenge={challenge}
          submitLabel={`Confirm booking · ${formatPrice(total)}`}
          onClose={() => setChallenge(null)}
          onVerify={(code) => book.mutateAsync(code)}
        >
          <div className="rounded-lg border border-zinc-200 px-3.5 py-3 text-sm">
            <p className="font-medium">{event.name}</p>
            <p className="mt-0.5 text-zinc-500">
              Seats {heldSeats.map((seat) => seat.seatNumber).join(', ')} · <span className="font-medium text-zinc-900">{formatPrice(total)}</span>
            </p>
          </div>
        </VerificationDialog>
      )}
    </div>
  )
}

function CheckoutSkeleton() {
  return (
    <div className="mx-auto max-w-2xl space-y-4 pt-9" aria-busy>
      <Skeleton className="h-8 w-64" />
      <Skeleton className="h-10 w-full" />
      <Skeleton className="h-64 w-full rounded-xl" />
    </div>
  )
}
