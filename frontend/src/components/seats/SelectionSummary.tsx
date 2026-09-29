import { X } from 'lucide-react'
import type { Seat } from '../../lib/types'
import { formatPrice, pluralize, SECTION_LABELS } from '../../lib/format'
import { Button } from '../ui/Button'

interface SelectionSummaryProps {
  seats: Seat[]
  maxSeats: number
  onRemove: (seat: Seat) => void
  onContinue: () => void
  continuing: boolean
  disabled: boolean
}

export function SelectionSummary({ seats, maxSeats, onRemove, onContinue, continuing, disabled }: SelectionSummaryProps) {
  const subtotal = seats.reduce((sum, seat) => sum + seat.price, 0)

  return (
    <>
      {/* Desktop: sticky side panel */}
      <aside className="hidden lg:block" aria-label="Booking summary">
        <div className="card sticky top-20 p-5">
          <div className="flex items-baseline justify-between">
            <h2 className="text-base font-semibold">Selected seats</h2>
            <span className="text-[13px] text-zinc-500">
              {seats.length}/{maxSeats}
            </span>
          </div>

          {seats.length === 0 ? (
            <p className="mt-4 text-sm text-zinc-500">Select seats on the map to continue.</p>
          ) : (
            <ul className="mt-4 max-h-72 space-y-1 overflow-y-auto">
              {seats.map((seat) => (
                <li key={seat.id} className="group flex items-center justify-between rounded-lg py-1.5 text-sm">
                  <span>
                    <span className="font-medium">{seat.seatNumber}</span>
                    <span className="text-zinc-500"> · {SECTION_LABELS[seat.section]}</span>
                  </span>
                  <span className="flex items-center gap-2">
                    <span className="tabular-nums">{formatPrice(seat.price)}</span>
                    <button
                      type="button"
                      onClick={() => onRemove(seat)}
                      className="rounded-md p-0.5 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700"
                      aria-label={`Remove seat ${seat.seatNumber}`}
                    >
                      <X className="size-3.5" />
                    </button>
                  </span>
                </li>
              ))}
            </ul>
          )}

          <div className="mt-4 flex items-baseline justify-between border-t border-zinc-100 pt-4">
            <span className="text-sm text-zinc-600">Subtotal</span>
            <span className="text-lg font-semibold tabular-nums">{formatPrice(subtotal)}</span>
          </div>
          <Button className="mt-4 w-full" size="lg" onClick={onContinue} loading={continuing} disabled={disabled || seats.length === 0}>
            Continue to Booking
          </Button>
          <p className="mt-3 text-center text-xs text-zinc-500">Seats are held for 5 minutes while you review.</p>
        </div>
      </aside>

      {/* Mobile: compact sticky bar, only while something is selected */}
      {seats.length > 0 && (
        <div className="fixed inset-x-0 bottom-0 z-30 border-t border-zinc-200 bg-white/95 px-4 py-3 backdrop-blur lg:hidden">
          <div className="mx-auto flex max-w-[1280px] items-center justify-between gap-4">
            <div className="min-w-0">
              <p className="text-sm font-semibold tabular-nums">
                {pluralize(seats.length, 'Seat')} • {formatPrice(subtotal)}
              </p>
              <p className="truncate text-xs text-zinc-500">{seats.map((seat) => seat.seatNumber).join(', ')}</p>
            </div>
            <Button onClick={onContinue} loading={continuing} disabled={disabled}>
              Continue
            </Button>
          </div>
        </div>
      )}
    </>
  )
}
