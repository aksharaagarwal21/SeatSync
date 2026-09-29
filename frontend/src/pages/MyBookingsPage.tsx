import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Ticket } from 'lucide-react'
import { bookingApi, queryKeys } from '../api/queries'
import { PageHeader } from '../components/layout/AppLayout'
import { BookingCard } from '../components/bookings/BookingCard'
import { Button, ButtonLink } from '../components/ui/Button'
import { Skeleton } from '../components/ui/Skeleton'
import { EmptyState, ErrorState } from '../components/ui/States'
import { useToast } from '../context/ToastContext'
import { errorMessage } from '../lib/api'
import type { Booking } from '../lib/types'

export function MyBookingsPage() {
  const queryClient = useQueryClient()
  const toast = useToast()

  const { data, isPending, isError, error, refetch, fetchNextPage, hasNextPage, isFetchingNextPage } = useInfiniteQuery({
    queryKey: queryKeys.myBookings,
    queryFn: ({ pageParam }) => bookingApi.mine(pageParam),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.page + 1),
  })

  const cancel = useMutation({
    mutationFn: (booking: Booking) => bookingApi.cancel(booking.id),
    onSuccess: (cancelled) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.myBookings })
      queryClient.invalidateQueries({ queryKey: queryKeys.seats(cancelled.eventId) })
      queryClient.setQueryData(queryKeys.booking(cancelled.id), cancelled)
      toast.success(`Booking ${cancelled.reference} cancelled. Your seats have been released.`)
    },
    onError: (err) => toast.warning(errorMessage(err)),
  })

  const bookings = data?.pages.flatMap((page) => page.content) ?? []

  return (
    <>
      <PageHeader title="My Bookings" />
      {isPending ? (
        <div className="space-y-3" aria-busy>
          {Array.from({ length: 3 }, (_, index) => (
            <Skeleton key={index} className="h-28 w-full rounded-xl" />
          ))}
        </div>
      ) : isError ? (
        <ErrorState message={errorMessage(error, 'We couldn’t load your bookings.')} onRetry={() => refetch()} />
      ) : bookings.length === 0 ? (
        <EmptyState icon={Ticket} title="You haven't booked any events yet." action={<ButtonLink to="/">Browse Events</ButtonLink>} />
      ) : (
        <div className="space-y-3">
          {bookings.map((booking) => (
            <BookingCard
              key={booking.id}
              booking={booking}
              onCancel={(target) => cancel.mutate(target)}
              cancelling={cancel.isPending && cancel.variables?.id === booking.id}
            />
          ))}
          {hasNextPage && (
            <div className="flex justify-center pt-3">
              <Button variant="secondary" onClick={() => fetchNextPage()} loading={isFetchingNextPage}>
                Show older bookings
              </Button>
            </div>
          )}
        </div>
      )}
    </>
  )
}
