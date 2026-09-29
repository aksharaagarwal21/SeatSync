import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/layout/AppLayout'
import { RequireAuth } from './components/layout/RequireAuth'
import { EventsPage } from './pages/EventsPage'
import { EventDetailPage } from './pages/EventDetailPage'
import { SeatSelectionPage } from './pages/SeatSelectionPage'
import { CheckoutPage } from './pages/CheckoutPage'
import { BookingConfirmationPage } from './pages/BookingConfirmationPage'
import { MyBookingsPage } from './pages/MyBookingsPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { NotFoundPage } from './pages/NotFoundPage'

// The admin area (and Recharts) is only downloaded by admins.
const AdminRoutes = lazy(() => import('./pages/admin/AdminRoutes'))

export function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<EventsPage />} />
        <Route path="events/:eventId" element={<EventDetailPage />} />
        <Route path="events/:eventId/seats" element={<SeatSelectionPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />

        <Route element={<RequireAuth />}>
          <Route path="events/:eventId/checkout" element={<CheckoutPage />} />
          <Route path="bookings" element={<MyBookingsPage />} />
          <Route path="bookings/:bookingId" element={<BookingConfirmationPage />} />
        </Route>

        <Route element={<RequireAuth adminOnly />}>
          <Route
            path="admin/*"
            element={
              <Suspense fallback={null}>
                <AdminRoutes />
              </Suspense>
            }
          />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
