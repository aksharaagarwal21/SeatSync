import { useId } from 'react'
import { Link } from 'react-router-dom'
import { cn } from '../../lib/cn'

/** 3 × 3 auditorium; the centre seat is "your seat". `i` staggers the diagonal selection wave. */
const SEATS = [0, 1, 2].flatMap((row) =>
  [0, 1, 2].map((col) => ({ x: 8.3 + col * 5.6, y: 12.6 + row * 5, i: row + col, selected: row === 1 && col === 1 })),
)

interface LogoMarkProps {
  size?: number
  /** `hover`: animates when an ancestor `.logo` is hovered or focused. `loop`: animates continuously (loaders). */
  animate?: 'hover' | 'loop'
  className?: string
}

export function LogoMark({ size = 30, animate = 'hover', className }: LogoMarkProps) {
  const id = useId().replace(/[^a-zA-Z0-9_-]/g, '')
  return (
    <svg
      viewBox="0 0 32 32"
      width={size}
      height={size}
      className={cn('logo-mark shrink-0', className)}
      data-animate={animate}
      aria-hidden
    >
      <defs>
        <linearGradient id={`${id}-tile`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#27272a" />
          <stop offset="1" stopColor="#09090b" />
        </linearGradient>
        <radialGradient id={`${id}-glow`} cx="0.5" cy="0.62" r="0.5">
          <stop offset="0" stopColor="#22c55e" stopOpacity="0.35" />
          <stop offset="1" stopColor="#22c55e" stopOpacity="0" />
        </radialGradient>
      </defs>
      <rect width="32" height="32" rx="9" fill={`url(#${id}-tile)`} />
      <rect className="logo-glow" width="32" height="32" rx="9" fill={`url(#${id}-glow)`} />
      <path className="logo-stage" d="M8.3 8.6 Q16 4.6 23.7 8.6" pathLength={1} fill="none" stroke="#fafafa" strokeWidth="1.8" strokeLinecap="round" />
      {SEATS.map((seat) =>
        seat.selected ? (
          <g key="selected">
            <rect className="logo-ping" x={seat.x} y={seat.y} width="4.2" height="3.6" rx="1.2" fill="none" stroke="#22c55e" strokeWidth="1" />
            <rect className="logo-seat-selected" x={seat.x} y={seat.y} width="4.2" height="3.6" rx="1.2" fill="#22c55e" />
          </g>
        ) : (
          <rect
            key={`${seat.x}-${seat.y}`}
            className="logo-seat"
            x={seat.x}
            y={seat.y}
            width="4.2"
            height="3.6"
            rx="1.2"
            style={{ '--i': seat.i } as React.CSSProperties}
          />
        ),
      )}
    </svg>
  )
}

export function Logo() {
  return (
    <Link to="/" className="logo flex items-center gap-2.5 rounded-lg" aria-label="SeatSync home">
      <LogoMark />
      <span className="text-[15px] font-semibold tracking-tight">
        <span className="text-zinc-900">Seat</span>
        <span className="logo-sync">Sync</span>
      </span>
    </Link>
  )
}

/** Branded loading indicator for full-page waits (auth check, lazy admin bundle). */
export function LogoLoader({ label = 'Loading' }: { label?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-24" role="status" aria-live="polite">
      <LogoMark size={40} animate="loop" />
      <span className="text-[13px] text-zinc-500">{label}…</span>
    </div>
  )
}
