import { useState } from 'react'
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Ticket } from 'lucide-react'
import { bookingApi, queryKeys, verificationApi } from '../api/queries'
import { PageHeader } from '../components/layout/AppLayout'
import { BookingCard } from '../components/bookings/BookingCard'
import { Button, ButtonLink } from '../components/ui/Button'
import { Skeleton } from '../components/ui/Skeleton'
import { EmptyState, ErrorState } from '../components/ui/States'
import { VerificationDialog } from '../components/verification/VerificationDialog'
import { useToast } from '../context/ToastContext'
import { errorMessage } from '../lib/api'
import { formatPrice } from '../lib/format'
import type { Booking, VerificationChallenge, VerificationCode } from '../lib/types'

export function MyBookingsPage() {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [pending, setPending] = useState<{ booking: Booking; challenge: VerificationChallenge } | null>(null)

  const { data, isPending, isError, error, refetch, fetchNextPage, hasNextPage, isFetchingNextPage } = useInfiniteQuery({
    queryKey: queryKeys.myBookings,
    queryFn: ({ pageParam }) => bookingApi.mine(pageParam),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.page + 1),
  })

  const cancel = useMutation({
    mutationFn: ({ booking, verification }: { booking: Booking; verification?: VerificationCode }) =>
      bookingApi.cancel(booking.id, verification),
    onSuccess: (cancelled) => {
      setPending(null)
      queryClient.invalidateQueries({ queryKey: queryKeys.myBookings })
      queryClient.invalidateQueries({ queryKey: queryKeys.seats(cancelled.eventId) })
      queryClient.setQueryData(queryKeys.booking(cancelled.id), cancelled)
      toast.success(`Booking ${cancelled.reference} cancelled. Your seats have been released.`)
    },
  })

  // Step-up verification: the emailed code approves cancelling this one booking.
  const requestCode = useMutation({
    mutationFn: (booking: Booking) => verificationApi.request({ purpose: 'CANCELLATION', bookingId: booking.id }),
    onSuccess: (challenge, booking) =>
      challenge.required
        ? setPending({ booking, challenge })
        : cancel.mutate({ booking }, { onError: (err) => toast.warning(errorMessage(err)) }),
    onError: (err) => toast.warning(errorMessage(err)),
  })

  const bookings = data?.pages.flatMap((page) => page.content) ?? []
  const busyId = requestCode.isPending ? requestCode.variables?.id : cancel.isPending && !pending ? cancel.variables?.booking.id : undefined

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
              onCancel={(target) => requestCode.mutate(target)}
              cancelling={busyId === booking.id}
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

      {pending && (
        <VerificationDialog
          title="Confirm cancellation"
          challenge={pending.challenge}
          submitLabel="Cancel booking"
          onClose={() => setPending(null)}
          onVerify={(verification) => cancel.mutateAsync({ booking: pending.booking, verification })}
        >
          <div className="rounded-lg border border-zinc-200 px-3.5 py-3 text-sm">
            <p className="font-medium">{pending.booking.eventName}</p>
            <p className="mt-0.5 text-zinc-500">
              {pending.booking.reference} · Seats {pending.booking.seats.map((seat) => seat.seatNumber).join(', ')} ·{' '}
              {formatPrice(pending.booking.totalAmount)}
            </p>
          </div>
        </VerificationDialog>
      )}
    </>
  )
}
