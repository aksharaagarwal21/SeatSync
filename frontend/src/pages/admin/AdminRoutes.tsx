import { NavLink, Outlet, Route, Routes } from 'react-router-dom'
import { cn } from '../../lib/cn'
import { AdminOverviewPage } from './AdminOverviewPage'
import { AdminEventsPage } from './AdminEventsPage'
import { AdminEventFormPage } from './AdminEventFormPage'
import { AdminBookingsPage } from './AdminBookingsPage'
import { AdminSeatsPage } from './AdminSeatsPage'
import { AdminUsersPage } from './AdminUsersPage'

const TABS = [
  { to: '/admin', label: 'Overview', end: true },
  { to: '/admin/events', label: 'Events', end: false },
  { to: '/admin/bookings', label: 'Bookings', end: false },
  { to: '/admin/seats', label: 'Seats', end: false },
  { to: '/admin/users', label: 'Users', end: false },
]

function AdminLayout() {
  return (
    <>
      <nav aria-label="Admin" className="-mx-4 mb-6 overflow-x-auto border-b border-zinc-200 px-4 sm:mx-0 sm:px-0">
        <ul className="flex gap-1">
          {TABS.map((tab) => (
            <li key={tab.to}>
              <NavLink
                to={tab.to}
                end={tab.end}
                className={({ isActive }) =>
                  cn(
                    '-mb-px inline-block border-b-2 px-3 py-2.5 text-sm font-medium whitespace-nowrap transition-colors',
                    isActive ? 'border-brand-700 text-zinc-900' : 'border-transparent text-zinc-500 hover:text-zinc-900',
                  )
                }
              >
                {tab.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <Outlet />
    </>
  )
}

export default function AdminRoutes() {
  return (
    <Routes>
      <Route element={<AdminLayout />}>
        <Route index element={<AdminOverviewPage />} />
        <Route path="events" element={<AdminEventsPage />} />
        <Route path="events/new" element={<AdminEventFormPage />} />
        <Route path="events/:eventId/edit" element={<AdminEventFormPage />} />
        <Route path="bookings" element={<AdminBookingsPage />} />
        <Route path="seats" element={<AdminSeatsPage />} />
        <Route path="users" element={<AdminUsersPage />} />
      </Route>
    </Routes>
  )
}
