import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useInfiniteQuery } from '@tanstack/react-query'
import { CalendarX2 } from 'lucide-react'
import { eventsApi, queryKeys, type EventSearch } from '../api/queries'
import { EventCard, EventCardSkeleton } from '../components/events/EventCard'
import { EventFilters } from '../components/events/EventFilters'
import { EMPTY_FILTERS, type DatePreset, type FilterState } from '../lib/eventFilters'
import { Button } from '../components/ui/Button'
import { EmptyState, ErrorState } from '../components/ui/States'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { errorMessage } from '../lib/api'
import { pluralize, toIsoDate } from '../lib/format'
import type { EventCategory } from '../lib/types'

export function EventsPage() {
  const [params, setParams] = useSearchParams()
  const filters: FilterState = {
    q: params.get('q') ?? '',
    category: (params.get('category') ?? '') as EventCategory | '',
    city: params.get('city') ?? '',
    date: (params.get('date') ?? '') as DatePreset,
  }
  const debouncedQuery = useDebouncedValue(filters.q.trim(), 300)

  const search = useMemo<EventSearch>(
    () => ({ q: debouncedQuery, category: filters.category, city: filters.city, ...dateRange(filters.date) }),
    [debouncedQuery, filters.category, filters.city, filters.date],
  )

  const { data, isPending, isError, error, refetch, fetchNextPage, hasNextPage, isFetchingNextPage } = useInfiniteQuery({
    queryKey: queryKeys.events(search),
    queryFn: ({ pageParam }) => eventsApi.search(search, pageParam),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.page + 1),
    staleTime: 30_000,
  })

  const events = data?.pages.flatMap((page) => page.content) ?? []
  const total = data?.pages[0]?.totalElements ?? 0

  const setFilters = (next: FilterState) => {
    const nextParams = new URLSearchParams()
    Object.entries(next).forEach(([key, value]) => value && nextParams.set(key, value))
    setParams(nextParams, { replace: true })
  }

  return (
    <>
      <section className="mb-6">
        <h1 className="text-[28px] leading-tight font-semibold tracking-tight">SeatSync</h1>
        <p className="mt-1 text-sm text-zinc-500">Find events. Choose your seat. Book securely.</p>
      </section>

      <EventFilters value={filters} onChange={setFilters} />

      <div className="mt-6">
        {isPending ? (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3" aria-busy>
            {Array.from({ length: 6 }, (_, index) => (
              <EventCardSkeleton key={index} />
            ))}
          </div>
        ) : isError ? (
          <ErrorState message={errorMessage(error, 'We couldn’t load events.')} onRetry={() => refetch()} />
        ) : events.length === 0 ? (
          <EmptyState
            icon={CalendarX2}
            title="No events match your filters"
            description="Try another date, city or category."
            action={
              <Button variant="secondary" onClick={() => setFilters(EMPTY_FILTERS)}>
                Clear filters
              </Button>
            }
          />
        ) : (
          <>
            <p className="mb-3 text-[13px] text-zinc-500" aria-live="polite">
              {pluralize(total, 'event')}
            </p>
            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {events.map((event) => (
                <EventCard key={event.id} event={event} />
              ))}
            </div>
            {hasNextPage && (
              <div className="mt-8 flex justify-center">
                <Button variant="secondary" onClick={() => fetchNextPage()} loading={isFetchingNextPage}>
                  Show more events
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </>
  )
}

function dateRange(preset: DatePreset): Pick<EventSearch, 'from' | 'to'> {
  const today = new Date()
  const plusDays = (days: number) => new Date(today.getFullYear(), today.getMonth(), today.getDate() + days)
  switch (preset) {
    case 'today':
      return { from: toIsoDate(today), to: toIsoDate(today) }
    case 'weekend': {
      const day = today.getDay()
      const saturday = day === 0 ? plusDays(-1) : plusDays(6 - day)
      const sunday = day === 0 ? today : plusDays(7 - day)
      return { from: toIsoDate(saturday < today ? today : saturday), to: toIsoDate(sunday) }
    }
    case 'week':
      return { from: toIsoDate(today), to: toIsoDate(plusDays(7)) }
    case 'month':
      return { from: toIsoDate(today), to: toIsoDate(plusDays(30)) }
    default:
      return {}
  }
}
