import type { Location } from 'react-router-dom'

/** Where to send the user after signing in: back to the page that asked them to. */
export function redirectTarget(location: Location) {
  const from = (location.state as { from?: Location } | null)?.from
  return from ? `${from.pathname}${from.search}` : '/'
}
