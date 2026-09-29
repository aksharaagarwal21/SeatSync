import { useEffect, useRef, type ClipboardEvent, type KeyboardEvent } from 'react'
import { cn } from '../../lib/cn'

const LENGTH = 6

interface OtpCodeInputProps {
  value: string
  onChange: (value: string) => void
  onComplete: (code: string) => void
  disabled?: boolean
  invalid?: boolean
  autoFocus?: boolean
}

/**
 * Six single-digit boxes. Typing advances, Backspace steps back, arrows move, and pasting a whole
 * code fills every box. The first box carries autocomplete="one-time-code" so phones can offer the
 * code from the email or SMS keyboard suggestion.
 */
export function OtpCodeInput({ value, onChange, onComplete, disabled, invalid, autoFocus = true }: OtpCodeInputProps) {
  const inputs = useRef<Array<HTMLInputElement | null>>([])
  const digits = Array.from({ length: LENGTH }, (_, index) => value[index] ?? '')
  const isEmpty = value === ''

  // Focus the next empty box whenever the field is enabled or cleared (e.g. after a wrong code).
  useEffect(() => {
    if (autoFocus && !disabled && isEmpty) inputs.current[0]?.focus()
  }, [autoFocus, disabled, isEmpty])

  const commit = (next: string) => {
    const clean = next.replace(/\D/g, '').slice(0, LENGTH)
    onChange(clean)
    if (clean.length === LENGTH) onComplete(clean)
    return clean
  }

  const handleInput = (index: number, raw: string) => {
    const typed = raw.replace(/\D/g, '')
    if (!typed) return
    // Autofill may drop the whole code into one box.
    const next = typed.length > 1 ? typed : value.slice(0, index) + typed + value.slice(index + 1)
    const clean = commit(next)
    inputs.current[Math.min(typed.length > 1 ? clean.length : index + 1, LENGTH - 1)]?.focus()
  }

  const handleKeyDown = (index: number, event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Backspace') {
      event.preventDefault()
      if (digits[index]) {
        commit(value.slice(0, index) + value.slice(index + 1))
      } else if (index > 0) {
        commit(value.slice(0, index - 1) + value.slice(index))
        inputs.current[index - 1]?.focus()
      }
    } else if (event.key === 'ArrowLeft' && index > 0) {
      inputs.current[index - 1]?.focus()
    } else if (event.key === 'ArrowRight' && index < LENGTH - 1) {
      inputs.current[index + 1]?.focus()
    }
  }

  const handlePaste = (event: ClipboardEvent<HTMLInputElement>) => {
    event.preventDefault()
    const clean = commit(event.clipboardData.getData('text'))
    inputs.current[Math.min(clean.length, LENGTH - 1)]?.focus()
  }

  return (
    <div className="flex justify-between gap-2" role="group" aria-label="Verification code">
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(element) => {
            inputs.current[index] = element
          }}
          value={digit}
          onChange={(event) => handleInput(index, event.target.value)}
          onKeyDown={(event) => handleKeyDown(index, event)}
          onPaste={handlePaste}
          onFocus={(event) => event.target.select()}
          disabled={disabled}
          inputMode="numeric"
          autoComplete={index === 0 ? 'one-time-code' : 'off'}
          maxLength={LENGTH}
          aria-label={`Digit ${index + 1} of ${LENGTH}`}
          aria-invalid={invalid || undefined}
          className={cn(
            'h-12 w-full max-w-12 rounded-lg border bg-white text-center text-xl font-semibold tabular-nums text-zinc-900 transition-colors',
            'focus:border-brand-600 focus:ring-2 focus:ring-brand-600/20 focus:outline-none disabled:bg-zinc-50 disabled:text-zinc-400',
            invalid ? 'border-red-400' : digit ? 'border-zinc-400' : 'border-zinc-300',
          )}
        />
      ))}
    </div>
  )
}
