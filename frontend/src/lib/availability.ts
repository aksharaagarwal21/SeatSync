import type { EventSummary } from './types'
import type { BadgeTone } from '../components/ui/Badge'

/** How an event card communicates whether seats can still be booked. */
export function availabilityOf(event: Pick<EventSummary, 'bookingOpen' | 'availableSeats' | 'totalSeats' | 'status'>): {
  label: string
  tone: BadgeTone
  bookable: boolean
} {
  if (event.status === 'PAUSED') return { label: 'Sales paused', tone: 'muted', bookable: false }
  if (!event.bookingOpen) return { label: 'Booking closed', tone: 'muted', bookable: false }
  if (event.availableSeats === 0) return { label: 'Sold out', tone: 'danger', bookable: false }
  if (event.availableSeats / Math.max(event.totalSeats, 1) <= 0.15) {
    return { label: `${event.availableSeats} left`, tone: 'warning', bookable: true }
  }
  return { label: 'Available', tone: 'success', bookable: true }
}
