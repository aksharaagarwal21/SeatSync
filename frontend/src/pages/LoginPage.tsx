import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { AuthCard } from '../components/auth/AuthCard'
import { Button } from '../components/ui/Button'
import { TextField } from '../components/ui/Field'
import { VerificationPanel } from '../components/verification/VerificationPanel'
import { useAuth } from '../context/AuthContext'
import { errorMessage } from '../lib/api'
import { redirectTarget } from '../lib/redirect'
import type { VerificationChallenge } from '../lib/types'

export function LoginPage() {
  const { user, login, verify } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [challenge, setChallenge] = useState<VerificationChallenge | null>(null)

  if (user) return <Navigate to={redirectTarget(location)} replace />

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const step = await login(email.trim(), password)
      if (step.status === 'verify') setChallenge(step.challenge)
      else navigate(redirectTarget(location), { replace: true })
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  if (challenge) {
    return (
      <AuthCard title="Check your email" subtitle="Enter the code to finish signing in.">
        <div className="mt-6">
          <VerificationPanel
            challenge={challenge}
            submitLabel="Verify and sign in"
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
            <ArrowLeft className="size-3.5" aria-hidden /> Use a different account
          </button>
        </div>
      </AuthCard>
    )
  }

  return (
    <AuthCard title="Sign in" subtitle="Welcome back to SeatSync.">
      <form onSubmit={handleSubmit} className="mt-6 space-y-4" noValidate>
        {error && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </p>
        )}
        <TextField label="Email" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        <TextField
          label="Password"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <Button type="submit" className="w-full" loading={submitting} disabled={!email || !password}>
          Sign In
        </Button>
        <p className="text-center text-xs text-zinc-500">We’ll email you a one-time code to confirm it’s you.</p>
      </form>
      <p className="mt-5 text-center text-sm text-zinc-500">
        New to SeatSync?{' '}
        <Link to="/register" state={location.state} className="rounded-sm font-medium text-brand-700 hover:text-brand-800">
          Create Account
        </Link>
      </p>
    </AuthCard>
  )
}
