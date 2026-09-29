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

  const startSession = useCallback(
    (response: LoginResponse) => {
      queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== 'events' && query.queryKey[0] !== 'event' })
      queryClient.setQueryData(SESSION_KEY, response.user)
      return response.user
    },
    [queryClient],
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
    queryClient.clear()
    queryClient.setQueryData(SESSION_KEY, null)
  }, [queryClient])

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
