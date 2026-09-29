import { memo } from 'react'
import { Link } from 'react-router-dom'
import { CalendarDays, MapPin } from 'lucide-react'
import type { EventSummary } from '../../lib/types'
import { CATEGORY_LABELS, formatPrice, formatShortDate, formatTime } from '../../lib/format'
import { EventImage } from './EventImage'
import { AvailabilityBadge } from './AvailabilityBadge'
import { availabilityOf } from '../../lib/availability'
import { buttonClasses } from '../ui/buttonClasses'
import { Skeleton } from '../ui/Skeleton'

export const EventCard = memo(function EventCard({ event }: { event: EventSummary }) {
  const { bookable } = availabilityOf(event)
  return (
    <article className="group card flex flex-col overflow-hidden transition-shadow hover:shadow-raised">
      <Link to={`/events/${event.id}`} className="relative block focus-visible:outline-offset-[-2px]" tabIndex={-1} aria-hidden>
        <EventImage src={event.imageUrl} alt="" category={event.category} className="aspect-[2/1] w-full" />
        <span className="absolute top-3 left-3 rounded-md bg-white/95 px-2 py-0.5 text-xs font-medium text-zinc-700 shadow-sm">
          {CATEGORY_LABELS[event.category]}
        </span>
      </Link>
      <div className="flex flex-1 flex-col p-4">
        <div className="flex items-start justify-between gap-3">
          <h3 className="text-base leading-snug font-semibold text-zinc-900">
            <Link to={`/events/${event.id}`} className="rounded-sm hover:text-brand-800">
              {event.name}
            </Link>
          </h3>
          <AvailabilityBadge event={event} />
        </div>
        <dl className="mt-2 space-y-1 text-[13px] text-zinc-500">
          <div className="flex items-center gap-1.5">
            <CalendarDays className="size-3.5 shrink-0" aria-hidden />
            <dt className="sr-only">Date</dt>
            <dd>
              {formatShortDate(event.eventDate)} • {formatTime(event.startTime)}
            </dd>
          </div>
          <div className="flex items-center gap-1.5">
            <MapPin className="size-3.5 shrink-0" aria-hidden />
            <dt className="sr-only">Venue</dt>
            <dd className="truncate">
              {event.venue}, {event.city}
            </dd>
          </div>
        </dl>
        <div className="mt-auto flex items-center justify-between pt-4">
          <p className="text-sm text-zinc-500">
            From <span className="font-semibold text-zinc-900">{formatPrice(event.minPrice)}</span>
          </p>
          <Link
            to={bookable ? `/events/${event.id}/seats` : `/events/${event.id}`}
            className={buttonClasses(bookable ? 'primary' : 'secondary', 'sm')}
            aria-label={`${bookable ? 'View seats' : 'View details'} for ${event.name}`}
          >
            {bookable ? 'View Seats' : 'Details'}
          </Link>
        </div>
      </div>
    </article>
  )
})

export function EventCardSkeleton() {
  return (
    <div className="card overflow-hidden">
      <Skeleton className="aspect-[2/1] w-full rounded-none" />
      <div className="space-y-2.5 p-4">
        <Skeleton className="h-5 w-3/4" />
        <Skeleton className="h-3.5 w-1/2" />
        <Skeleton className="h-3.5 w-2/3" />
        <div className="flex justify-between pt-3">
          <Skeleton className="h-5 w-20" />
          <Skeleton className="h-8 w-24" />
        </div>
      </div>
    </div>
  )
}
