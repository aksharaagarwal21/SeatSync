import { memo, useCallback, useLayoutEffect, useMemo, useRef, useState, type KeyboardEvent } from 'react'
import { Lock, X } from 'lucide-react'
import type { Seat, SeatSection } from '../../lib/types'
import { formatPrice, SECTION_LABELS } from '../../lib/format'
import { cn } from '../../lib/cn'

type SeatVisualState = 'available' | 'selected' | 'booked' | 'held'

interface SeatMapProps {
  seats: Seat[]
  selectedIds: ReadonlySet<number>
  onToggle?: (seat: Seat) => void
  /** Admin monitoring: shows the same map without selection. */
  readOnly?: boolean
}

interface Row {
  label: string
  section: SeatSection
  seats: Seat[]
}

function seatVisualState(seat: Seat, selected: boolean): SeatVisualState {
  if (selected) return 'selected'
  if (seat.status === 'BOOKED') return 'booked'
  if (seat.status === 'RESERVED' && !seat.heldByMe) return 'held'
  return 'available'
}

const STATE_LABELS: Record<SeatVisualState, string> = {
  available: 'available',
  selected: 'selected',
  booked: 'booked',
  held: 'temporarily held by another customer',
}

export function SeatMap({ seats, selectedIds, onToggle, readOnly = false }: SeatMapProps) {
  const rows = useMemo(() => groupRows(seats), [seats])
  const seatsPerRow = rows[0]?.seats.length ?? 0
  const aisles = useMemo(() => aislePositions(seatsPerRow), [seatsPerRow])
  const buttons = useRef(new Map<number, HTMLButtonElement>())
  const containerRef = useRef<HTMLDivElement>(null)
  const [focusedId, setFocusedId] = useState<number | null>(null)
  const [tooltip, setTooltip] = useState<{ seat: Seat; left: number; top: number } | null>(null)

  // On narrow screens the map scrolls sideways; start centred on the stage like a real auditorium view.
  const hasSeats = seats.length > 0
  useLayoutEffect(() => {
    const container = containerRef.current
    if (container && hasSeats) container.scrollLeft = (container.scrollWidth - container.clientWidth) / 2
  }, [hasSeats])

  const tabbableId = focusedId ?? rows.flatMap((row) => row.seats).find((seat) => seat.status === 'AVAILABLE')?.id ?? seats[0]?.id

  const showTooltip = useCallback((seat: Seat, element: HTMLElement) => {
    const container = containerRef.current
    if (!container) return
    const box = container.getBoundingClientRect()
    const target = element.getBoundingClientRect()
    setTooltip({ seat, left: target.left - box.left + container.scrollLeft + target.width / 2, top: target.top - box.top + container.scrollTop })
  }, [])

  const hideTooltip = useCallback(() => setTooltip(null), [])

  const handleToggle = useCallback(
    (seat: Seat) => {
      if (!readOnly && onToggle) onToggle(seat)
    },
    [onToggle, readOnly],
  )

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    const moves: Record<string, [number, number]> = { ArrowLeft: [0, -1], ArrowRight: [0, 1], ArrowUp: [-1, 0], ArrowDown: [1, 0] }
    const move = moves[event.key]
    if (!move || tabbableId == null) return
    event.preventDefault()
    const rowIndex = rows.findIndex((row) => row.seats.some((seat) => seat.id === tabbableId))
    const colIndex = rows[rowIndex].seats.findIndex((seat) => seat.id === tabbableId)
    const nextRow = rows[Math.min(Math.max(rowIndex + move[0], 0), rows.length - 1)]
    const nextSeat = nextRow.seats[Math.min(Math.max(colIndex + move[1], 0), nextRow.seats.length - 1)]
    setFocusedId(nextSeat.id)
    buttons.current.get(nextSeat.id)?.focus()
  }

  const registerButton = useCallback((id: number, element: HTMLButtonElement | null) => {
    if (element) buttons.current.set(id, element)
    else buttons.current.delete(id)
  }, [])

  return (
    <div ref={containerRef} className="relative overflow-x-auto pb-2" onMouseLeave={hideTooltip}>
      <div className="mx-auto w-max min-w-full px-2">
        <Stage />
        <div role="grid" aria-label="Seat map" aria-readonly={readOnly || undefined} onKeyDown={handleKeyDown} className="mt-6 flex flex-col items-center gap-1 sm:gap-1.5">
          {rows.map((row, index) => (
            <div key={row.label} className="contents">
              {(index === 0 || rows[index - 1].section !== row.section) && <SectionDivider section={row.section} seats={row.seats} first={index === 0} />}
              <div role="row" aria-label={`Row ${row.label}`} className="flex items-center gap-1">
                <RowLabel label={row.label} />
                {row.seats.map((seat, seatIndex) => (
                  <div key={seat.id} role="gridcell" className={cn('flex', aisles.has(seatIndex) && 'ml-3 sm:ml-4')}>
                    <SeatButton
                      seat={seat}
                      state={seatVisualState(seat, selectedIds.has(seat.id))}
                      tabbable={seat.id === tabbableId}
                      readOnly={readOnly}
                      onToggle={handleToggle}
                      onFocusSeat={setFocusedId}
                      onShowTooltip={showTooltip}
                      onHideTooltip={hideTooltip}
                      registerButton={registerButton}
                    />
                  </div>
                ))}
                <RowLabel label={row.label} />
              </div>
            </div>
          ))}
        </div>
      </div>
      {tooltip && <SeatTooltip {...tooltip} selected={selectedIds.has(tooltip.seat.id)} />}
    </div>
  )
}

interface SeatButtonProps {
  seat: Seat
  state: SeatVisualState
  tabbable: boolean
  readOnly: boolean
  onToggle: (seat: Seat) => void
  onFocusSeat: (id: number) => void
  onShowTooltip: (seat: Seat, element: HTMLElement) => void
  onHideTooltip: () => void
  registerButton: (id: number, element: HTMLButtonElement | null) => void
}

const SEAT_STYLES: Record<SeatVisualState, string> = {
  available: 'border-zinc-300 bg-white text-zinc-600 hover:border-brand-600 hover:bg-brand-50 hover:text-brand-800',
  selected: 'border-brand-700 bg-brand-700 text-white shadow-sm',
  booked: 'cursor-not-allowed border-zinc-200 bg-zinc-200 text-zinc-400',
  held: 'cursor-not-allowed border-amber-300 bg-amber-100 text-amber-700',
}

const SeatButton = memo(function SeatButton({
  seat,
  state,
  tabbable,
  readOnly,
  onToggle,
  onFocusSeat,
  onShowTooltip,
  onHideTooltip,
  registerButton,
}: SeatButtonProps) {
  const unavailable = state === 'booked' || state === 'held'
  return (
    <button
      type="button"
      ref={(element) => registerButton(seat.id, element)}
      tabIndex={tabbable ? 0 : -1}
      aria-disabled={unavailable || readOnly || undefined}
      aria-pressed={readOnly ? undefined : state === 'selected'}
      aria-label={`Seat ${seat.seatNumber}, ${SECTION_LABELS[seat.section]}, ${formatPrice(seat.price)}, ${STATE_LABELS[state]}`}
      onClick={() => !unavailable && onToggle(seat)}
      onFocus={(event) => {
        onFocusSeat(seat.id)
        onShowTooltip(seat, event.currentTarget)
      }}
      onBlur={onHideTooltip}
      onMouseEnter={(event) => onShowTooltip(seat, event.currentTarget)}
      className={cn(
        'flex size-7 items-center justify-center rounded-t-[9px] rounded-b-[5px] border text-[10px] font-medium tabular-nums transition-colors sm:size-8 sm:text-[11px]',
        readOnly && 'cursor-default',
        SEAT_STYLES[state],
      )}
    >
      {state === 'booked' ? (
        <X className="size-3" aria-hidden />
      ) : state === 'held' ? (
        <Lock className="size-3" aria-hidden />
      ) : (
        seat.number
      )}
    </button>
  )
})

function SeatTooltip({ seat, left, top, selected }: { seat: Seat; left: number; top: number; selected: boolean }) {
  const state = seatVisualState(seat, selected)
  return (
    <div
      role="tooltip"
      className="pointer-events-none absolute z-10 -translate-x-1/2 -translate-y-full rounded-lg bg-zinc-900 px-2.5 py-1.5 text-xs whitespace-nowrap text-white shadow-raised"
      style={{ left, top: top - 6 }}
    >
      <span className="font-semibold">{seat.seatNumber}</span>
      <span className="text-zinc-400"> · {SECTION_LABELS[seat.section]} · </span>
      <span className="font-medium">{formatPrice(seat.price)}</span>
      {(state === 'booked' || state === 'held') && (
        <span className="mt-0.5 block text-zinc-400">{state === 'booked' ? 'Booked' : 'Held by another customer'}</span>
      )}
    </div>
  )
}

function Stage() {
  return (
    <div className="mx-auto flex max-w-md flex-col items-center" aria-hidden>
      <svg viewBox="0 0 400 24" className="h-5 w-full text-zinc-300" preserveAspectRatio="none">
        <path d="M4 20 Q200 -6 396 20" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
      </svg>
      <span className="mt-1 text-[11px] font-semibold tracking-[0.3em] text-zinc-400">STAGE</span>
    </div>
  )
}

function SectionDivider({ section, seats, first }: { section: SeatSection; seats: Seat[]; first: boolean }) {
  return (
    <div className={cn('flex w-full items-center gap-3 px-8 text-[11px] font-medium text-zinc-500', first ? 'mb-1' : 'mt-3 mb-1')}>
      <span className="h-px flex-1 bg-zinc-200" />
      {SECTION_LABELS[section]} · {formatPrice(seats[0]?.price ?? 0)}
      <span className="h-px flex-1 bg-zinc-200" />
    </div>
  )
}

function RowLabel({ label }: { label: string }) {
  return (
    <span className="w-5 text-center text-[11px] font-semibold text-zinc-400" aria-hidden>
      {label}
    </span>
  )
}

function groupRows(seats: Seat[]): Row[] {
  const rows = new Map<string, Row>()
  for (const seat of seats) {
    const row = rows.get(seat.row) ?? { label: seat.row, section: seat.section, seats: [] }
    row.seats.push(seat)
    rows.set(seat.row, row)
  }
  return [...rows.values()]
    .sort((a, b) => a.label.localeCompare(b.label))
    .map((row) => ({ ...row, seats: [...row.seats].sort((a, b) => a.number - b.number) }))
}

/** Splits wide rows into left block · centre block · right block, like an auditorium. */
function aislePositions(seatsPerRow: number) {
  if (seatsPerRow < 10) return new Set<number>()
  const side = Math.round(seatsPerRow / 4)
  return new Set([side, seatsPerRow - side])
}
