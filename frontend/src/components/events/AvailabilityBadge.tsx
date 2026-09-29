import type { EventSummary } from '../../lib/types'
import { availabilityOf } from '../../lib/availability'
import { Badge } from '../ui/Badge'

export function AvailabilityBadge({ event }: { event: EventSummary }) {
  const { label, tone } = availabilityOf(event)
  return <Badge tone={tone}>{label}</Badge>
}
