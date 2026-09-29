import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { queryKeys } from '../api/queries'

/**
 * Subscribes to the server's seat stream for one event. Each message means "seats changed",
 * so we refetch the seat map (which carries per-user fields like heldByMe) and the event
 * summary. EventSource reconnects on its own; polling in useSeats covers any gap.
 */
export function useSeatUpdates(eventId: number) {
  const queryClient = useQueryClient()

  useEffect(() => {
    if (!Number.isFinite(eventId) || typeof EventSource === 'undefined') return

    const source = new EventSource(`/api/events/${eventId}/seats/stream`)
    const refresh = () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.seats(eventId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.event(eventId) })
    }
    source.addEventListener('seats', refresh)
    return () => {
      source.removeEventListener('seats', refresh)
      source.close()
    }
  }, [eventId, queryClient])
}
