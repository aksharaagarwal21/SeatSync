import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from 'react'
import { AlertTriangle, CheckCircle2, X } from 'lucide-react'
import { cn } from '../lib/cn'

type ToastTone = 'success' | 'warning'

interface Toast {
  id: number
  tone: ToastTone
  message: string
}

interface ToastContextValue {
  success: (message: string) => void
  warning: (message: string) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)
const TOAST_DURATION_MS = 5000

/** Toasts are reserved for outcomes the user should notice: bookings, cancellations, lost seats. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const nextId = useRef(0)

  const dismiss = useCallback((id: number) => setToasts((current) => current.filter((toast) => toast.id !== id)), [])

  const show = useCallback(
    (tone: ToastTone, message: string) => {
      const id = ++nextId.current
      setToasts((current) => [...current.filter((toast) => toast.message !== message), { id, tone, message }].slice(-3))
      window.setTimeout(() => dismiss(id), TOAST_DURATION_MS)
    },
    [dismiss],
  )

  const value = useMemo(
    () => ({ success: (message: string) => show('success', message), warning: (message: string) => show('warning', message) }),
    [show],
  )

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div
        aria-live="polite"
        className="pointer-events-none fixed inset-x-0 bottom-20 z-50 flex flex-col items-center gap-2 px-4 sm:bottom-6 sm:items-end sm:px-6"
      >
        {toasts.map((toast) => (
          <div
            key={toast.id}
            role="status"
            className={cn(
              'pointer-events-auto flex w-full max-w-sm animate-toast-in items-start gap-3 rounded-xl border bg-white p-3.5 text-sm shadow-raised',
              toast.tone === 'success' ? 'border-brand-200' : 'border-amber-200',
            )}
          >
            {toast.tone === 'success' ? (
              <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-brand-600" aria-hidden />
            ) : (
              <AlertTriangle className="mt-0.5 size-4 shrink-0 text-amber-600" aria-hidden />
            )}
            <p className="flex-1 text-zinc-800">{toast.message}</p>
            <button
              type="button"
              onClick={() => dismiss(toast.id)}
              className="-m-1 rounded-md p-1 text-zinc-400 hover:text-zinc-700"
              aria-label="Dismiss notification"
            >
              <X className="size-4" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useToast() {
  const context = useContext(ToastContext)
  if (!context) throw new Error('useToast must be used inside ToastProvider')
  return context
}
