import { useQuery } from '@tanstack/react-query'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis, type TooltipProps } from 'recharts'
import { adminApi, queryKeys } from '../../api/queries'
import { AdminTable, TableSkeleton } from '../../components/admin/AdminTable'
import { BookingStatusBadge } from '../../components/bookings/BookingStatusBadge'
import { Skeleton } from '../../components/ui/Skeleton'
import { ErrorState } from '../../components/ui/States'
import { errorMessage } from '../../lib/api'
import { formatDateTime, formatPrice, formatShortDate } from '../../lib/format'
import type { AdminDashboard } from '../../lib/types'

const numberFormat = new Intl.NumberFormat('en-IN')

export function AdminOverviewPage() {
  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: queryKeys.adminDashboard,
    queryFn: adminApi.dashboard,
    refetchInterval: 30_000,
  })

  if (isError) return <ErrorState message={errorMessage(error, 'We couldn’t load the dashboard.')} onRetry={() => refetch()} />

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatTile label="Total events" value={data?.totalEvents} detail={data && `${data.upcomingEvents} upcoming`} />
        <StatTile label="Confirmed bookings" value={data?.confirmedBookings} />
        <StatTile label="Tickets sold" value={data?.ticketsSold} />
        <StatTile label="Available seats" value={data?.availableSeats} detail="Across upcoming events" />
      </div>

      <section className="card p-5" aria-labelledby="tickets-chart-heading">
        <h2 id="tickets-chart-heading" className="text-base font-semibold">
          Tickets sold · last 14 days
        </h2>
        {data ? <TicketsChart data={data.dailyBookings} /> : <Skeleton className="mt-4 h-56 w-full" />}
      </section>

      <section aria-labelledby="recent-heading">
        <h2 id="recent-heading" className="mb-3 text-base font-semibold">
          Recent bookings
        </h2>
        {isPending ? (
          <TableSkeleton rows={5} />
        ) : (
          <AdminTable caption="Most recent bookings" head={['Booking', 'Customer', 'Event', 'Seats', 'Amount', 'Status', 'Booked']}>
            {data.recentBookings.map((booking) => (
              <tr key={booking.id}>
                <td className="px-4 py-3 font-mono text-xs">{booking.reference}</td>
                <td className="px-4 py-3">{booking.customerName}</td>
                <td className="max-w-56 truncate px-4 py-3">{booking.eventName}</td>
                <td className="px-4 py-3 text-zinc-600">{booking.seatNumbers.join(', ')}</td>
                <td className="px-4 py-3 tabular-nums">{formatPrice(booking.totalAmount)}</td>
                <td className="px-4 py-3">
                  <BookingStatusBadge status={booking.status} />
                </td>
                <td className="px-4 py-3 whitespace-nowrap text-zinc-500">{formatDateTime(booking.bookingTime)}</td>
              </tr>
            ))}
          </AdminTable>
        )}
      </section>
    </div>
  )
}

function StatTile({ label, value, detail }: { label: string; value?: number; detail?: string | false }) {
  return (
    <div className="card p-4">
      <p className="text-[13px] text-zinc-500">{label}</p>
      {value === undefined ? (
        <Skeleton className="mt-2 h-7 w-20" />
      ) : (
        <p className="mt-1 text-2xl font-semibold tracking-tight tabular-nums">{numberFormat.format(value)}</p>
      )}
      {detail && <p className="mt-0.5 text-xs text-zinc-500">{detail}</p>}
    </div>
  )
}

function TicketsChart({ data }: { data: AdminDashboard['dailyBookings'] }) {
  return (
    <>
      <div className="mt-4 h-56" aria-hidden>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 4, right: 4, bottom: 0, left: -12 }} barCategoryGap={3}>
            <CartesianGrid vertical={false} stroke="#f4f4f5" />
            <XAxis
              dataKey="date"
              tickFormatter={formatShortDate}
              tick={{ fontSize: 11, fill: '#71717a' }}
              tickLine={false}
              axisLine={{ stroke: '#e4e4e7' }}
              interval="preserveStartEnd"
              minTickGap={16}
            />
            <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: '#71717a' }} tickLine={false} axisLine={false} width={40} />
            <Tooltip cursor={{ fill: '#f4f4f5' }} content={<ChartTooltip />} />
            <Bar dataKey="tickets" fill="#16a34a" radius={[4, 4, 0, 0]} maxBarSize={28} />
          </BarChart>
        </ResponsiveContainer>
      </div>
      <table className="sr-only">
        <caption>Tickets and bookings per day for the last 14 days</caption>
        <thead>
          <tr>
            <th scope="col">Date</th>
            <th scope="col">Tickets</th>
            <th scope="col">Bookings</th>
          </tr>
        </thead>
        <tbody>
          {data.map((day) => (
            <tr key={day.date}>
              <td>{formatShortDate(day.date)}</td>
              <td>{day.tickets}</td>
              <td>{day.bookings}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  )
}

function ChartTooltip({ active, payload }: TooltipProps<number, string>) {
  if (!active || !payload?.length) return null
  const day = payload[0].payload as AdminDashboard['dailyBookings'][number]
  return (
    <div className="rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs shadow-raised">
      <p className="font-medium text-zinc-900">{formatShortDate(day.date)}</p>
      <p className="mt-0.5 text-zinc-600">
        <span className="font-semibold text-zinc-900 tabular-nums">{day.tickets}</span> tickets ·{' '}
        <span className="tabular-nums">{day.bookings}</span> bookings
      </p>
    </div>
  )
}
