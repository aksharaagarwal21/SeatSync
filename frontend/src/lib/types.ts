export type Role = 'USER' | 'ADMIN'

export type EventCategory = 'CONCERT' | 'COMEDY' | 'THEATRE' | 'SPORTS' | 'CONFERENCE' | 'WORKSHOP'
export type EventStatus = 'ON_SALE' | 'PAUSED'
export type SeatStatus = 'AVAILABLE' | 'RESERVED' | 'BOOKED'
export type SeatSection = 'VIP' | 'PREMIUM' | 'STANDARD'
export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED'

export interface User {
  id: number
  name: string
  email: string
  role: Role
}

export interface LoginResponse {
  token: string
  expiresAt: string
  user: User
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface EventSummary {
  id: number
  name: string
  category: EventCategory
  venue: string
  city: string
  eventDate: string
  startTime: string
  imageUrl: string | null
  status: EventStatus
  bookingOpen: boolean
  minPrice: number
  maxPrice: number
  totalSeats: number
  availableSeats: number
}

export interface SectionAvailability {
  section: SeatSection
  price: number
  totalSeats: number
  availableSeats: number
}

export interface EventDetail extends EventSummary {
  description: string
  sections: SectionAvailability[]
}

export interface EventFilters {
  categories: EventCategory[]
  cities: string[]
}

export interface Seat {
  id: number
  seatNumber: string
  row: string
  number: number
  section: SeatSection
  price: number
  status: SeatStatus
  heldByMe: boolean
  holdExpiresAt: string | null
}

export interface HoldResponse {
  eventId: number
  expiresAt: string
  seats: Seat[]
  totalAmount: number
}

export interface BookedSeat {
  seatId: number
  seatNumber: string
  section: SeatSection
  price: number
}

export interface Booking {
  id: number
  reference: string
  eventId: number
  eventName: string
  venue: string
  city: string
  eventDate: string
  startTime: string
  imageUrl: string | null
  seats: BookedSeat[]
  totalAmount: number
  status: BookingStatus
  bookingTime: string
  cancellable: boolean
}

export interface SectionPricing {
  vip: number
  premium: number
  standard: number
}

export interface AdminEvent {
  id: number
  name: string
  description: string
  category: EventCategory
  venue: string
  city: string
  eventDate: string
  startTime: string
  imageUrl: string | null
  status: EventStatus
  rows: number
  seatsPerRow: number
  pricing: SectionPricing
  totalSeats: number
  availableSeats: number
  heldSeats: number
  bookedSeats: number
}

export interface EventRequest {
  name: string
  description: string
  category: EventCategory
  venue: string
  city: string
  eventDate: string
  startTime: string
  imageUrl: string
  bookingOpen: boolean
  rows: number
  seatsPerRow: number
  pricing: SectionPricing
}

export interface AdminBooking {
  id: number
  reference: string
  customerName: string
  customerEmail: string
  eventId: number
  eventName: string
  eventDate: string
  seatNumbers: string[]
  totalAmount: number
  status: BookingStatus
  bookingTime: string
}

export interface AdminUser {
  id: number
  name: string
  email: string
  role: Role
  createdAt: string
  confirmedBookings: number
}

export interface DailyBookings {
  date: string
  bookings: number
  tickets: number
}

export interface AdminDashboard {
  totalEvents: number
  upcomingEvents: number
  confirmedBookings: number
  ticketsSold: number
  availableSeats: number
  dailyBookings: DailyBookings[]
  recentBookings: AdminBooking[]
}
