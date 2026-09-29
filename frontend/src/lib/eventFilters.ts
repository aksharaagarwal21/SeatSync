import type { EventCategory } from './types'

export type DatePreset = '' | 'today' | 'weekend' | 'week' | 'month'

export interface FilterState {
  q: string
  category: EventCategory | ''
  city: string
  date: DatePreset
}

export const EMPTY_FILTERS: FilterState = { q: '', category: '', city: '', date: '' }
