import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { ArrowLeft, CircleAlert } from 'lucide-react'
import { bookingApi, useEvent, useSeats } from '../api/queries'
import { SeatLegend } from '../components/seats/SeatLegend'
import { SeatMap } from '../components/seats/SeatMap'
import { SelectionSummary } from '../components/seats/SelectionSummary'
import { Skeleton } from '../components/ui/Skeleton'
import { ErrorState } from '../components/ui/States'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'
import { useSeatUpdates } from '../hooks/useSeatUpdates'
import { ApiError, errorMessage } from '../lib/api'
import { formatLongDate, formatTime } from '../lib/format'
import type { EventDetail, Seat } from '../lib/types'

const MAX_SEATS = 10
const pendingSelectionKey = (eventId: number) => `seatsync:selection:${eventId}`

export function SeatSelectionPage() {
  const eventId = Number(useParams().eventId)
  const navigate = useNavigate()
  const location = useLocation()
  const toast = useToast()
  const { user } = useAuth()
  const { data: event, isError: eventError, error: eventErr, refetch: refetchEvent } = useEvent(eventId)
  const { data: seats, isPending, isError, error, refetch } = useSeats(eventId)
  useSeatUpdates(eventId)

  const [selectedIds, setSelectedIds] = useState<Set<number>>(() => restorePendingSelection(eventId))
  const initialised = useRef(false)
  useEffect(() => sessionStorage.removeItem(pendingSelectionKey(eventId)), [eventId])
  const seatsById = useMemo(() => new Map((seats ?? []).map((seat) => [seat.id, seat])), [seats])

  // First load: keep seats this user is already holding (e.g. returning from checkout).
  useEffect(() => {
    if (!seats || initialised.current) return
    initialised.current = true
    const held = seats.filter((seat) => seat.heldByMe).map((seat) => seat.id)
    if (held.length) setSelectedIds((current) => new Set([...current, ...held]))
  }, [seats])

  // Whenever availability changes, drop selected seats someone else just took and say which ones.
  useEffect(() => {
    if (!seats) return
    const lost = [...selectedIds]
      .map((id) => seatsById.get(id))
      .filter((seat): seat is Seat => !!seat && seat.status !== 'AVAILABLE' && !seat.heldByMe)
    if (lost.length === 0) return
    setSelectedIds((current) => new Set([...current].filter((id) => !lost.some((seat) => seat.id === id))))
    lost.forEach((seat) =>
      toast.warning(
        seat.status === 'BOOKED'
          ? `Seat ${seat.seatNumber} was just booked by another user.`
          : `Seat ${seat.seatNumber} is being held by another user.`,
      ),
    )
  }, [seats, seatsById, selectedIds, toast])

  const toggleSeat = useCallback(
    (seat: Seat) => {
      setSelectedIds((current) => {
        const next = new Set(current)
        if (next.has(seat.id)) {
          next.delete(seat.id)
        } else if (next.size >= MAX_SEATS) {
          toast.warning(`You can select up to ${MAX_SEATS} seats per booking.`)
          return current
        } else {
          next.add(seat.id)
        }
        return next
      })
    },
    [toast],
  )

  const hold = useMutation({
    mutationFn: () => bookingApi.hold(eventId, [...selectedIds]),
    onSuccess: () => navigate(`/events/${eventId}/checkout`),
    onError: async (err) => {
      if (err instanceof ApiError && err.isConflict) {
        const { data: fresh } = await refetch()
        const stillFree = fresh?.every((seat) => !selectedIds.has(seat.id) || seat.status === 'AVAILABLE' || seat.heldByMe)
        if (stillFree) toast.warning(err.message)
        return
      }
      toast.warning(errorMessage(err))
    },
  })

  const handleContinue = () => {
    if (!user) {
      sessionStorage.setItem(pendingSelectionKey(eventId), JSON.stringify([...selectedIds]))
      navigate('/login', { state: { from: location } })
      return
    }
    hold.mutate()
  }

  if (isError || eventError) {
    const err = eventError ? eventErr : error
    return <ErrorState message={errorMessage(err, 'We couldn’t load the seat map.')} onRetry={() => (eventError ? refetchEvent() : refetch())} />
  }

  const selectedSeats = [...selectedIds].map((id) => seatsById.get(id)).filter((seat): seat is Seat => !!seat)
  const soldOut = !!seats && seats.every((seat) => seat.status !== 'AVAILABLE' && !seat.heldByMe)
  const closed = event ? !event.bookingOpen : false

  return (
    <div className="pb-20 lg:pb-0">
      <Link to={`/events/${eventId}`} className="mb-4 inline-flex items-center gap-1.5 rounded-md text-sm text-zinc-500 hover:text-zinc-900">
        <ArrowLeft className="size-4" aria-hidden /> Event details
      </Link>

      <EventHeading event={event} />

      <div className="mt-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
        <section className="card min-w-0 p-4 sm:p-6" aria-labelledby="seat-map-heading">
          <h2 id="seat-map-heading" className="sr-only">
            Choose your seats
          </h2>
          <SeatLegend className="mb-6 justify-center" />

          {(soldOut || closed) && (
            <p className="mb-6 flex items-center justify-center gap-2 rounded-lg bg-zinc-50 px-4 py-3 text-sm text-zinc-600">
              <CircleAlert className="size-4 text-zinc-400" aria-hidden />
              {closed ? 'Booking is closed for this event.' : 'This event is currently sold out.'}
            </p>
          )}

          {isPending ? (
            <SeatMapSkeleton />
          ) : (
            <SeatMap seats={seats} selectedIds={selectedIds} onToggle={toggleSeat} readOnly={closed} />
          )}
          <p className="mt-4 hidden text-center text-xs text-zinc-400 sm:block">Use arrow keys to move between seats and Enter to select.</p>
        </section>

        <SelectionSummary
          seats={selectedSeats}
          maxSeats={MAX_SEATS}
          onRemove={toggleSeat}
          onContinue={handleContinue}
          continuing={hold.isPending}
          disabled={closed}
        />
      </div>
    </div>
  )
}

function EventHeading({ event }: { event?: EventDetail }) {
  if (!event) {
    return (
      <div className="space-y-2">
        <Skeleton className="h-7 w-64" />
        <Skeleton className="h-4 w-80" />
      </div>
    )
  }
  return (
    <div>
      <h1 className="text-2xl leading-tight font-semibold tracking-tight sm:text-[28px]">{event.name}</h1>
      <p className="mt-1 text-sm text-zinc-500">
        {event.venue}, {event.city} · {formatLongDate(event.eventDate)} · {formatTime(event.startTime)}
      </p>
    </div>
  )
}

function SeatMapSkeleton() {
  return (
    <div className="flex flex-col items-center gap-2" aria-busy aria-label="Loading seats">
      <Skeleton className="mb-6 h-6 w-72" />
      {Array.from({ length: 8 }, (_, index) => (
        <Skeleton key={index} className="h-8 w-full max-w-2xl" />
      ))}
    </div>
  )
}

function restorePendingSelection(eventId: number) {
  try {
    const raw = sessionStorage.getItem(pendingSelectionKey(eventId))
    return new Set<number>(raw ? (JSON.parse(raw) as number[]) : [])
  } catch {
    return new Set<number>()
  }
}
