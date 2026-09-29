import type { BookingStatus } from '../../lib/types'
import { BOOKING_STATUS_LABELS } from '../../lib/format'
import { Badge, type BadgeTone } from '../ui/Badge'

const TONES: Record<BookingStatus, BadgeTone> = {
  PENDING: 'warning',
  CONFIRMED: 'success',
  CANCELLED: 'muted',
  COMPLETED: 'neutral',
}

export function BookingStatusBadge({ status }: { status: BookingStatus }) {
  return <Badge tone={TONES[status]}>{BOOKING_STATUS_LABELS[status]}</Badge>
}
