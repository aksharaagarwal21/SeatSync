import type { BookingStatus, EventCategory, SeatSection } from './types'

const currency = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })
const shortDate = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short' })
const longDate = new Intl.DateTimeFormat('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })
const dateTime = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' })

export function formatPrice(amount: number) {
  return currency.format(amount)
}

/** Event dates are venue-local calendar dates (YYYY-MM-DD) and must not be shifted by the browser's zone. */
function parseLocalDate(date: string) {
  const [year, month, day] = date.split('-').map(Number)
  return new Date(year, month - 1, day)
}

export function formatShortDate(date: string) {
  return shortDate.format(parseLocalDate(date))
}

export function formatLongDate(date: string) {
  return longDate.format(parseLocalDate(date))
}

export function formatTime(time: string) {
  const [hours, minutes] = time.split(':').map(Number)
  const suffix = hours >= 12 ? 'PM' : 'AM'
  const hour12 = hours % 12 === 0 ? 12 : hours % 12
  return `${hour12}:${String(minutes).padStart(2, '0')} ${suffix}`
}

export function formatDateTime(instant: string) {
  return dateTime.format(new Date(instant))
}

export function toIsoDate(date: Date) {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

export const CATEGORY_LABELS: Record<EventCategory, string> = {
  CONCERT: 'Concerts',
  COMEDY: 'Comedy',
  THEATRE: 'Theatre',
  SPORTS: 'Sports',
  CONFERENCE: 'Conferences',
  WORKSHOP: 'Workshops',
}

export const SECTION_LABELS: Record<SeatSection, string> = {
  VIP: 'VIP',
  PREMIUM: 'Premium',
  STANDARD: 'Standard',
}

export const BOOKING_STATUS_LABELS: Record<BookingStatus, string> = {
  PENDING: 'Pending',
  CONFIRMED: 'Confirmed',
  CANCELLED: 'Cancelled',
  COMPLETED: 'Completed',
}

export function pluralize(count: number, singular: string, plural = `${singular}s`) {
  return `${count} ${count === 1 ? singular : plural}`
}
