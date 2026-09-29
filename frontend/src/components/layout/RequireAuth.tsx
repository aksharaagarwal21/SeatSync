import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { LogoLoader } from '../brand/Logo'

export function RequireAuth({ adminOnly = false }: { adminOnly?: boolean }) {
  const { user, isAdmin, isLoading } = useAuth()
  const location = useLocation()

  if (isLoading) {
    return <LogoLoader label="Checking your session" />
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  if (adminOnly && !isAdmin) {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
