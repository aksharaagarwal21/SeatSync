import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { AuthCard } from '../components/auth/AuthCard'
import { Button } from '../components/ui/Button'
import { TextField } from '../components/ui/Field'
import { useAuth } from '../context/AuthContext'
import { errorMessage } from '../lib/api'
import { redirectTarget } from '../lib/redirect'

export function LoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (user) return <Navigate to={redirectTarget(location)} replace />

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(email.trim(), password)
      navigate(redirectTarget(location), { replace: true })
    } catch (err) {
      setError(errorMessage(err))
      setSubmitting(false)
    }
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
