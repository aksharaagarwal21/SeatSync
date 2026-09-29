export interface ApiErrorBody {
  timestamp?: string
  status: number
  error: string
  message: string
  fieldErrors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(body: ApiErrorBody) {
    super(body.message)
    this.name = 'ApiError'
    this.status = body.status
    this.fieldErrors = body.fieldErrors ?? {}
  }

  get isConflict() {
    return this.status === 409
  }
}

type QueryValue = string | number | boolean | null | undefined

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  query?: Record<string, QueryValue>
  signal?: AbortSignal
}

/** Auth travels in an HttpOnly cookie, so requests never touch the token directly. */
export async function api<T>(path: string, { method = 'GET', body, query, signal }: RequestOptions = {}): Promise<T> {
  const response = await fetch(`/api${path}${toQueryString(query)}`, {
    method,
    signal,
    credentials: 'same-origin',
    headers: body === undefined ? { Accept: 'application/json' } : { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (response.status === 204) {
    return undefined as T
  }

  const payload = await response.json().catch(() => null)
  if (!response.ok) {
    throw new ApiError(
      payload?.message
        ? payload
        : { status: response.status, error: response.statusText, message: fallbackMessage(response.status) },
    )
  }
  return payload as T
}

export function errorMessage(error: unknown, fallback = 'Something went wrong. Please try again.') {
  if (error instanceof ApiError) return error.message
  if (error instanceof TypeError) return 'Can’t reach SeatSync right now. Check your connection and try again.'
  return fallback
}

function toQueryString(query?: Record<string, QueryValue>) {
  if (!query) return ''
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') params.set(key, String(value))
  }
  const text = params.toString()
  return text ? `?${text}` : ''
}

function fallbackMessage(status: number) {
  if (status === 401) return 'Please sign in to continue.'
  if (status === 403) return 'You don’t have permission to do that.'
  if (status === 404) return 'We couldn’t find what you were looking for.'
  if (status >= 500) return 'SeatSync is having trouble right now. Please try again in a moment.'
  return 'The request couldn’t be completed.'
}
