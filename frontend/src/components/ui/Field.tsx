import { forwardRef, useId, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'

interface FieldShellProps {
  id: string
  label: string
  error?: string
  hint?: ReactNode
  children: ReactNode
  className?: string
}

function FieldShell({ id, label, error, hint, children, className }: FieldShellProps) {
  return (
    <div className={className}>
      <label htmlFor={id} className="label">
        {label}
      </label>
      {children}
      {error ? (
        <p id={`${id}-error`} className="mt-1.5 text-[13px] text-red-600">
          {error}
        </p>
      ) : (
        hint && <p className="mt-1.5 text-[13px] text-zinc-500">{hint}</p>
      )}
    </div>
  )
}

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  hint?: ReactNode
  containerClassName?: string
}

export const TextField = forwardRef<HTMLInputElement, TextFieldProps>(function TextField(
  { label, error, hint, containerClassName, className, id, ...props },
  ref,
) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  return (
    <FieldShell id={inputId} label={label} error={error} hint={hint} className={containerClassName}>
      <input
        ref={ref}
        id={inputId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${inputId}-error` : undefined}
        className={cn('input', className)}
        {...props}
      />
    </FieldShell>
  )
})

interface SelectFieldProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  error?: string
  containerClassName?: string
}

export function SelectField({ label, error, containerClassName, className, id, children, ...props }: SelectFieldProps) {
  const generatedId = useId()
  const selectId = id ?? generatedId
  return (
    <FieldShell id={selectId} label={label} error={error} className={containerClassName}>
      <select id={selectId} aria-invalid={error ? true : undefined} className={cn('input pr-8', className)} {...props}>
        {children}
      </select>
    </FieldShell>
  )
}
