import { Search, X } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { eventsApi, queryKeys } from '../../api/queries'
import { CATEGORY_LABELS } from '../../lib/format'
import type { EventCategory } from '../../lib/types'
import { EMPTY_FILTERS, type DatePreset, type FilterState } from '../../lib/eventFilters'

const DATE_OPTIONS: Array<{ value: DatePreset; label: string }> = [
  { value: '', label: 'Any date' },
  { value: 'today', label: 'Today' },
  { value: 'weekend', label: 'This weekend' },
  { value: 'week', label: 'Next 7 days' },
  { value: 'month', label: 'Next 30 days' },
]

interface EventFiltersProps {
  value: FilterState
  onChange: (next: FilterState) => void
}

export function EventFilters({ value, onChange }: EventFiltersProps) {
  const { data: filters } = useQuery({ queryKey: queryKeys.eventFilters, queryFn: eventsApi.filters, staleTime: 5 * 60_000 })
  const update = (patch: Partial<FilterState>) => onChange({ ...value, ...patch })
  const hasFilters = value.q || value.category || value.city || value.date

  return (
    <div className="flex flex-col gap-2 sm:flex-row sm:items-center" role="search">
      <div className="relative flex-1">
        <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-zinc-400" aria-hidden />
        <input
          type="search"
          value={value.q}
          onChange={(event) => update({ q: event.target.value })}
          placeholder="Search events or venues"
          aria-label="Search events or venues"
          className="input pl-9"
        />
      </div>
      <div className="grid grid-cols-3 gap-2 sm:flex">
        <select
          value={value.category}
          onChange={(event) => update({ category: event.target.value as EventCategory | '' })}
          aria-label="Category"
          className="input sm:w-40"
        >
          <option value="">All categories</option>
          {(filters?.categories ?? Object.keys(CATEGORY_LABELS)).map((category) => (
            <option key={category} value={category}>
              {CATEGORY_LABELS[category as EventCategory]}
            </option>
          ))}
        </select>
        <select value={value.date} onChange={(event) => update({ date: event.target.value as DatePreset })} aria-label="Date" className="input sm:w-36">
          {DATE_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <select value={value.city} onChange={(event) => update({ city: event.target.value })} aria-label="City" className="input sm:w-36">
          <option value="">All cities</option>
          {filters?.cities.map((city) => (
            <option key={city} value={city}>
              {city}
            </option>
          ))}
        </select>
      </div>
      {hasFilters && (
        <button
          type="button"
          onClick={() => onChange(EMPTY_FILTERS)}
          className="inline-flex h-10 items-center gap-1 self-start rounded-lg px-2 text-sm text-zinc-500 hover:text-zinc-900 sm:self-auto"
        >
          <X className="size-4" aria-hidden /> Clear
        </button>
      )}
    </div>
  )
}
