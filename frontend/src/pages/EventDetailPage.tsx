import { useParams } from 'react-router-dom'
import { ArrowLeft, CalendarDays, Clock, MapPin } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useEvent } from '../api/queries'
import { AvailabilityBadge } from '../components/events/AvailabilityBadge'
import { availabilityOf } from '../lib/availability'
import { EventImage } from '../components/events/EventImage'
import { ButtonLink } from '../components/ui/Button'
import { Skeleton } from '../components/ui/Skeleton'
import { ErrorState } from '../components/ui/States'
import { errorMessage } from '../lib/api'
import { CATEGORY_LABELS, formatLongDate, formatPrice, formatTime, SECTION_LABELS } from '../lib/format'
import type { EventDetail } from '../lib/types'

export function EventDetailPage() {
  const eventId = Number(useParams().eventId)
  const { data: event, isPending, isError, error, refetch } = useEvent(eventId)

  if (isPending) return <EventDetailSkeleton />
  if (isError) return <ErrorState message={errorMessage(error, 'We couldn’t load this event.')} onRetry={() => refetch()} />

  const { bookable } = availabilityOf(event)

  return (
    <>
      <Link to="/" className="mb-5 inline-flex items-center gap-1.5 rounded-md text-sm text-zinc-500 hover:text-zinc-900">
        <ArrowLeft className="size-4" aria-hidden /> All events
      </Link>

      <div className="grid gap-6 md:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)] md:gap-10">
        <EventImage
          src={event.imageUrl}
          alt={event.name}
          category={event.category}
          eager
          width={1100}
          className="aspect-[16/10] w-full rounded-xl"
        />

        <div className="flex flex-col">
          <div className="flex items-center gap-2">
            <span className="text-[13px] font-medium text-zinc-500">{CATEGORY_LABELS[event.category]}</span>
            <AvailabilityBadge event={event} />
          </div>
          <h1 className="mt-2 text-[28px] leading-tight font-semibold tracking-tight md:text-[32px]">{event.name}</h1>

          <dl className="mt-5 space-y-2.5 text-sm text-zinc-700">
            <InfoRow icon={CalendarDays} label="Date" value={formatLongDate(event.eventDate)} />
            <InfoRow icon={Clock} label="Time" value={formatTime(event.startTime)} />
            <InfoRow icon={MapPin} label="Venue" value={`${event.venue}, ${event.city}`} />
          </dl>

          <p className="mt-5 text-sm leading-relaxed text-zinc-600">{event.description}</p>

          <div className="mt-6 flex flex-col gap-4 border-t border-zinc-200 pt-5 sm:flex-row sm:items-center sm:justify-between md:mt-auto">
            <div>
              <p className="text-[13px] text-zinc-500">Tickets</p>
              <p className="text-lg font-semibold">
                {event.minPrice === event.maxPrice
                  ? formatPrice(event.minPrice)
                  : `${formatPrice(event.minPrice)} – ${formatPrice(event.maxPrice)}`}
              </p>
            </div>
            {bookable ? (
              <ButtonLink to={`/events/${event.id}/seats`} size="lg">
                Choose Seats
              </ButtonLink>
            ) : (
              <p className="text-sm text-zinc-500">{closedReason(event)}</p>
            )}
          </div>
        </div>
      </div>

      <section className="mt-10" aria-labelledby="sections-heading">
        <h2 id="sections-heading" className="text-xl font-semibold tracking-tight">
          Seat availability
        </h2>
        <div className="mt-4 grid gap-3 sm:grid-cols-3">
          {event.sections.map((section) => {
            const ratio = section.totalSeats ? section.availableSeats / section.totalSeats : 0
            return (
              <div key={section.section} className="card p-4">
                <div className="flex items-baseline justify-between">
                  <p className="font-medium">{SECTION_LABELS[section.section]}</p>
                  <p className="text-sm font-semibold">{formatPrice(section.price)}</p>
                </div>
                <div
                  className="mt-3 h-1.5 overflow-hidden rounded-full bg-zinc-100"
                  role="meter"
                  aria-valuemin={0}
                  aria-valuemax={section.totalSeats}
                  aria-valuenow={section.availableSeats}
                  aria-label={`${SECTION_LABELS[section.section]} seats available`}
                >
                  <div className="h-full rounded-full bg-brand-600" style={{ width: `${ratio * 100}%` }} />
                </div>
                <p className="mt-2 text-[13px] text-zinc-500">
                  {section.availableSeats === 0 ? 'Sold out' : `${section.availableSeats} of ${section.totalSeats} available`}
                </p>
              </div>
            )
          })}
        </div>
      </section>
    </>
  )
}

function closedReason(event: EventDetail) {
  if (event.status === 'PAUSED') return 'Booking is currently paused for this event.'
  if (!event.bookingOpen) return 'This event has already started.'
  return 'This event is currently sold out.'
}

function InfoRow({ icon: Icon, label, value }: { icon: typeof CalendarDays; label: string; value: string }) {
  return (
    <div className="flex items-center gap-2.5">
      <Icon className="size-4 text-zinc-400" aria-hidden />
      <dt className="sr-only">{label}</dt>
      <dd>{value}</dd>
    </div>
  )
}

function EventDetailSkeleton() {
  return (
    <div aria-busy className="pt-9">
      <div className="grid gap-6 md:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)] md:gap-10">
        <Skeleton className="aspect-[16/10] w-full rounded-xl" />
        <div className="space-y-3">
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-8 w-3/4" />
          <Skeleton className="h-4 w-1/2" />
          <Skeleton className="h-4 w-1/3" />
          <Skeleton className="h-4 w-2/3" />
          <Skeleton className="mt-6 h-16 w-full" />
        </div>
      </div>
    </div>
  )
}
