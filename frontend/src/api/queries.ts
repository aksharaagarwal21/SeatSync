import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api } from '../lib/api'
import type {
  AdminBooking,
  AdminDashboard,
  AdminEvent,
  AdminUser,
  Booking,
  BookingStatus,
  EventCategory,
  EventDetail,
  EventFilters,
  EventRequest,
  EventSummary,
  HoldResponse,
  Page,
  Seat,
} from '../lib/types'

export interface EventSearch {
  q?: string
  category?: EventCategory | ''
  city?: string
  from?: string
  to?: string
}

export const queryKeys = {
  events: (search: EventSearch) => ['events', search] as const,
  eventFilters: ['events', 'filters'] as const,
  event: (id: number) => ['event', id] as const,
  seats: (eventId: number) => ['seats', eventId] as const,
  myBookings: ['bookings', 'me'] as const,
  booking: (id: number) => ['booking', id] as const,
  adminDashboard: ['admin', 'dashboard'] as const,
  adminEvents: (q: string, page: number) => ['admin', 'events', q, page] as const,
  adminEvent: (id: number) => ['admin', 'event', id] as const,
  adminBookings: (eventId: number | undefined, status: string, page: number) => ['admin', 'bookings', eventId, status, page] as const,
  adminUsers: (page: number) => ['admin', 'users', page] as const,
}

export const eventsApi = {
  search: (search: EventSearch, page: number) =>
    api<Page<EventSummary>>('/events', { query: { ...search, page, size: 12 } }),
  filters: () => api<EventFilters>('/events/filters'),
  get: (id: number) => api<EventDetail>(`/events/${id}`),
  seats: (id: number) => api<Seat[]>(`/events/${id}/seats`),
}

export const bookingApi = {
  hold: (eventId: number, seatIds: number[]) => api<HoldResponse>('/holds', { method: 'POST', body: { eventId, seatIds } }),
  releaseHold: (eventId: number) => api<void>('/holds', { method: 'DELETE', query: { eventId } }),
  create: (eventId: number, seatIds: number[]) => api<Booking>('/bookings', { method: 'POST', body: { eventId, seatIds } }),
  mine: (page: number) => api<Page<Booking>>('/bookings/me', { query: { page, size: 10 } }),
  get: (id: number) => api<Booking>(`/bookings/${id}`),
  cancel: (id: number) => api<Booking>(`/bookings/${id}`, { method: 'DELETE' }),
}

export const adminApi = {
  dashboard: () => api<AdminDashboard>('/admin/dashboard'),
  events: (q: string, page: number) => api<Page<AdminEvent>>('/admin/events', { query: { q, page, size: 20 } }),
  event: (id: number) => api<AdminEvent>(`/admin/events/${id}`),
  createEvent: (request: EventRequest) => api<AdminEvent>('/admin/events', { method: 'POST', body: request }),
  updateEvent: (id: number, request: EventRequest) => api<AdminEvent>(`/admin/events/${id}`, { method: 'PUT', body: request }),
  deleteEvent: (id: number) => api<void>(`/admin/events/${id}`, { method: 'DELETE' }),
  bookings: (eventId: number | undefined, status: BookingStatus | '', page: number) =>
    api<Page<AdminBooking>>('/admin/bookings', { query: { eventId, status, page, size: 20 } }),
  users: (page: number) => api<Page<AdminUser>>('/admin/users', { query: { page, size: 20 } }),
}

export function useEvent(eventId: number) {
  return useQuery({ queryKey: queryKeys.event(eventId), queryFn: () => eventsApi.get(eventId), staleTime: 30_000 })
}

/** Live data: refetched on SSE updates, with slow polling as a fallback when the stream drops. */
export function useSeats(eventId: number) {
  return useQuery({
    queryKey: queryKeys.seats(eventId),
    queryFn: () => eventsApi.seats(eventId),
    refetchInterval: 20_000,
    placeholderData: keepPreviousData,
  })
}
