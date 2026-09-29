import { useState, type ReactNode } from 'react'
import { MailCheck } from 'lucide-react'
import { verificationApi } from '../../api/queries'
import { formatCountdown, useCountdown } from '../../hooks/useCountdown'
import { errorMessage } from '../../lib/api'
import type { VerificationChallenge, VerificationCode } from '../../lib/types'
import { Button } from '../ui/Button'
import { OtpCodeInput } from './OtpCodeInput'

const IS_LOCAL = ['localhost', '127.0.0.1'].includes(window.location.hostname)

interface VerificationPanelProps {
  challenge: VerificationChallenge
  /** Performs the approved action with the code. Throw to show the error and let the user retry. */
  onVerify: (code: VerificationCode) => Promise<unknown>
  submitLabel: string
  /** What the code approves, shown above the input. */
  children?: ReactNode
}

/** Enter-the-emailed-code step, shared by sign-in, registration, booking and cancellation. */
export function VerificationPanel({ challenge: initialChallenge, onVerify, submitLabel, children }: VerificationPanelProps) {
  const [challenge, setChallenge] = useState(initialChallenge)
  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [resending, setResending] = useState(false)
  const resendIn = useCountdown(challenge.resendAvailableAt ?? null) ?? 0
  const expiresIn = useCountdown(challenge.expiresAt ?? null)
  const expired = expiresIn === 0

  const submit = async (value = code) => {
    if (value.length !== 6 || submitting) return
    setSubmitting(true)
    setError(null)
    setNotice(null)
    try {
      await onVerify({ challengeId: challenge.challengeId!, code: value })
    } catch (err) {
      setError(errorMessage(err))
      setCode('')
    } finally {
      setSubmitting(false)
    }
  }

  const resend = async () => {
    setResending(true)
    setError(null)
    try {
      setChallenge(await verificationApi.resend(challenge.challengeId!))
      setCode('')
      setNotice('We sent a new code. Earlier codes no longer work.')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setResending(false)
    }
  }

  return (
    <div>
      <div className="flex items-start gap-3 rounded-lg bg-zinc-50 px-3.5 py-3">
        <MailCheck className="mt-0.5 size-4 shrink-0 text-brand-700" aria-hidden />
        <p className="text-sm text-zinc-600">
          We emailed a 6-digit code to <span className="font-medium text-zinc-900">{challenge.maskedEmail}</span>.
          {IS_LOCAL && (
            <>
              {' '}Running locally? Open the test inbox at{' '}
              <a href="http://localhost:8025" target="_blank" rel="noreferrer" className="font-medium text-brand-700 underline-offset-2 hover:underline">
                localhost:8025
              </a>
              .
            </>
          )}
        </p>
      </div>

      {children && <div className="mt-4">{children}</div>}

      <form
        className="mt-5"
        onSubmit={(event) => {
          event.preventDefault()
          submit()
        }}
      >
        <OtpCodeInput value={code} onChange={setCode} onComplete={submit} disabled={submitting || expired} invalid={!!error} />

        <div className="mt-2 min-h-5 text-[13px]" aria-live="polite">
          {error ? (
            <p role="alert" className="text-red-600">{error}</p>
          ) : notice ? (
            <p className="text-brand-700">{notice}</p>
          ) : expired ? (
            <p className="text-amber-700">This code has expired. Request a new one below.</p>
          ) : (
            expiresIn !== null && <p className="text-zinc-500">Code expires in {formatCountdown(expiresIn)}</p>
          )}
        </div>

        <Button type="submit" size="lg" className="mt-3 w-full" loading={submitting} disabled={code.length !== 6 || expired}>
          {submitLabel}
        </Button>
      </form>

      <p className="mt-4 text-center text-sm text-zinc-500">
        Didn’t get it?{' '}
        {resendIn > 0 ? (
          <span className="tabular-nums">Resend in {resendIn}s</span>
        ) : (
          <button type="button" onClick={resend} disabled={resending} className="rounded-sm font-medium text-brand-700 hover:text-brand-800 disabled:text-zinc-400">
            {resending ? 'Sending…' : 'Resend code'}
          </button>
        )}
      </p>
    </div>
  )
}
