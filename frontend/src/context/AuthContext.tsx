import { createContext, useCallback, useContext, useMemo, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError } from '../lib/api'
import type { LoginResponse, User } from '../lib/types'

interface AuthContextValue {
  user: User | null
  isLoading: boolean
  isAdmin: boolean
  login: (email: string, password: string) => Promise<User>
  register: (name: string, email: string, password: string) => Promise<User>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)
const SESSION_KEY = ['auth', 'me'] as const
const USER_SCOPED_KEYS = new Set(['bookings', 'booking', 'admin', 'seats'])

/**
 * The JWT lives in an HttpOnly cookie the page can't read, so the session is restored
 * by asking the API who we are.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()

  const { data: user = null, isLoading } = useQuery({
    queryKey: SESSION_KEY,
    queryFn: async () => {
      try {
        return await api<User>('/auth/me')
      } catch (error) {
        if (error instanceof ApiError && (error.status === 401 || error.status === 404)) return null
        throw error
      }
    },
    staleTime: Infinity,
    retry: false,
  })

  /**
   * Updates the session query in place and resets data that belongs to the previous user (bookings,
   * admin views, seat maps with heldByMe flags). Queries are reset, never removed: a removed query
   * stops notifying the components subscribed to it, which left the UI signed out after login.
   */
  const switchUser = useCallback(
    (next: User | null) => {
      queryClient.setQueryData(SESSION_KEY, next)
      queryClient.resetQueries({ predicate: (query) => USER_SCOPED_KEYS.has(String(query.queryKey[0])) })
    },
    [queryClient],
  )

  const startSession = useCallback(
    (response: LoginResponse) => {
      switchUser(response.user)
      return response.user
    },
    [switchUser],
  )

  const login = useCallback(
    async (email: string, password: string) =>
      startSession(await api<LoginResponse>('/auth/login', { method: 'POST', body: { email, password } })),
    [startSession],
  )

  const register = useCallback(
    async (name: string, email: string, password: string) =>
      startSession(await api<LoginResponse>('/auth/register', { method: 'POST', body: { name, email, password } })),
    [startSession],
  )

  const logout = useCallback(async () => {
    await api<void>('/auth/logout', { method: 'POST' })
    switchUser(null)
  }, [switchUser])

  const value = useMemo(
    () => ({ user, isLoading, isAdmin: user?.role === 'ADMIN', login, register, logout }),
    [user, isLoading, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
