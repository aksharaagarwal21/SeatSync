import { useEffect, useId, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { ShieldCheck, X } from 'lucide-react'
import type { VerificationChallenge, VerificationCode } from '../../lib/types'
import { VerificationPanel } from './VerificationPanel'

interface VerificationDialogProps {
  title: string
  challenge: VerificationChallenge
  onVerify: (code: VerificationCode) => Promise<unknown>
  onClose: () => void
  submitLabel: string
  /** What the user is approving, e.g. seats and total. */
  children?: ReactNode
}

/** Modal step-up verification for sensitive actions (booking, cancellation). */
export function VerificationDialog({ title, challenge, onVerify, onClose, submitLabel, children }: VerificationDialogProps) {
  const titleId = useId()
  const dialogRef = useRef<HTMLDivElement>(null)
  const closeRef = useRef(onClose)
  useEffect(() => {
    closeRef.current = onClose
  })

  useEffect(() => {
    const previouslyFocused = document.activeElement as HTMLElement | null
    const { overflow } = document.body.style
    document.body.style.overflow = 'hidden'

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') closeRef.current()
      if (event.key !== 'Tab' || !dialogRef.current) return
      // Keep keyboard focus inside the dialog.
      const focusable = dialogRef.current.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([disabled]), a[href]')
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last?.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first?.focus()
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.body.style.overflow = overflow
      previouslyFocused?.focus()
    }
  }, [])

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center p-0 sm:items-center sm:p-4">
      <div className="absolute inset-0 animate-fade-in bg-zinc-900/40 backdrop-blur-[2px]" onClick={onClose} aria-hidden />
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="relative w-full max-w-md animate-toast-in rounded-t-2xl bg-white p-6 shadow-raised sm:rounded-2xl"
      >
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 rounded-md p-1 text-zinc-400 hover:bg-zinc-100 hover:text-zinc-700"
          aria-label="Close"
        >
          <X className="size-4" />
        </button>
        <div className="mb-4 flex items-center gap-2.5">
          <span className="flex size-9 items-center justify-center rounded-full bg-brand-50">
            <ShieldCheck className="size-4.5 text-brand-700" aria-hidden />
          </span>
          <h2 id={titleId} className="text-lg font-semibold tracking-tight">
            {title}
          </h2>
        </div>
        <VerificationPanel challenge={challenge} onVerify={onVerify} submitLabel={submitLabel}>
          {children}
        </VerificationPanel>
      </div>
    </div>,
    document.body,
  )
}
