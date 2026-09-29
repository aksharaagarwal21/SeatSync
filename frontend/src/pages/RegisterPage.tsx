import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { TextField } from '../components/ui/Field'
import { useAuth } from '../context/AuthContext'
import { ApiError, errorMessage } from '../lib/api'
import { AuthCard } from '../components/auth/AuthCard'
import { redirectTarget } from '../lib/redirect'
import { VerificationPanel } from '../components/verification/VerificationPanel'
import type { VerificationChallenge } from '../lib/types'
import { ArrowLeft } from 'lucide-react'

interface FormState {
  name: string
  email: string
  password: string
  confirmPassword: string
}

type FormErrors = Partial<Record<keyof FormState, string>>

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validate(form: FormState): FormErrors {
  const errors: FormErrors = {}
  if (form.name.trim().length < 2) errors.name = 'Enter your full name.'
  if (!EMAIL_PATTERN.test(form.email.trim())) errors.email = 'Enter a valid email address.'
  if (form.password.length < 8) errors.password = 'Use at least 8 characters.'
  else if (!/[A-Za-z]/.test(form.password) || !/\d/.test(form.password)) errors.password = 'Include at least one letter and one number.'
  if (form.confirmPassword !== form.password) errors.confirmPassword = 'Passwords don’t match.'
  return errors
}

export function RegisterPage() {
  const { user, register, verify } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState<FormState>({ name: '', email: '', password: '', confirmPassword: '' })
  const [errors, setErrors] = useState<FormErrors>({})
  const [touched, setTouched] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [challenge, setChallenge] = useState<VerificationChallenge | null>(null)

  if (user) return <Navigate to={redirectTarget(location)} replace />

  const update = (field: keyof FormState) => (event: React.ChangeEvent<HTMLInputElement>) => {
    const next = { ...form, [field]: event.target.value }
    setForm(next)
    if (touched) setErrors(validate(next))
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setTouched(true)
    setFormError(null)
    const validation = validate(form)
    setErrors(validation)
    if (Object.keys(validation).length) return

    setSubmitting(true)
    try {
      const step = await register(form.name.trim(), form.email.trim(), form.password)
      if (step.status === 'verify') setChallenge(step.challenge)
      else navigate(redirectTarget(location), { replace: true })
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length) setErrors(err.fieldErrors)
      else setFormError(errorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  if (challenge) {
    return (
      <AuthCard title="Verify your email" subtitle="Enter the code to activate your account.">
        <div className="mt-6">
          <VerificationPanel
            challenge={challenge}
            submitLabel="Verify and create account"
            onVerify={async (code) => {
              await verify(code)
              navigate(redirectTarget(location), { replace: true })
            }}
          />
          <button
            type="button"
            onClick={() => setChallenge(null)}
            className="mx-auto mt-3 flex items-center gap-1.5 rounded-md text-sm text-zinc-500 hover:text-zinc-900"
          >
            <ArrowLeft className="size-3.5" aria-hidden /> Change details
          </button>
        </div>
      </AuthCard>
    )
  }

  return (
    <AuthCard title="Create account" subtitle="Book seats in a few clicks.">
      <form onSubmit={handleSubmit} className="mt-6 space-y-4" noValidate>
        {formError && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {formError}
          </p>
        )}
        <TextField label="Full name" autoComplete="name" value={form.name} onChange={update('name')} error={errors.name} />
        <TextField label="Email" type="email" autoComplete="email" value={form.email} onChange={update('email')} error={errors.email} />
        <TextField
          label="Password"
          type="password"
          autoComplete="new-password"
          value={form.password}
          onChange={update('password')}
          error={errors.password}
          hint={!errors.password ? 'At least 8 characters, with a letter and a number.' : undefined}
        />
        <TextField
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          value={form.confirmPassword}
          onChange={update('confirmPassword')}
          error={errors.confirmPassword}
        />
        <Button type="submit" className="w-full" loading={submitting}>
          Create Account
        </Button>
      </form>
      <p className="mt-5 text-center text-sm text-zinc-500">
        Already have an account?{' '}
        <Link to="/login" state={location.state} className="rounded-sm font-medium text-brand-700 hover:text-brand-800">
          Sign in
        </Link>
      </p>
    </AuthCard>
  )
}
