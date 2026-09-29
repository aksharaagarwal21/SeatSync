import { useEffect, useState } from 'react'

/** Seconds remaining until `deadline`, updated every second; null when there is no deadline. */
export function useCountdown(deadline: string | null) {
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    if (!deadline) return
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(timer)
  }, [deadline])

  if (!deadline) return null
  return Math.max(0, Math.floor((new Date(deadline).getTime() - now) / 1000))
}

export function formatCountdown(seconds: number) {
  const minutes = Math.floor(seconds / 60)
  return `${minutes}:${String(seconds % 60).padStart(2, '0')}`
}
