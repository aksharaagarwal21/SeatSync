import { useState } from 'react'
import { Briefcase, Drama, Laugh, Music, Presentation, Ticket, Trophy, type LucideIcon } from 'lucide-react'
import type { EventCategory } from '../../lib/types'
import { cn } from '../../lib/cn'

const CATEGORY_ICONS: Record<EventCategory, LucideIcon> = {
  CONCERT: Music,
  COMEDY: Laugh,
  THEATRE: Drama,
  SPORTS: Trophy,
  CONFERENCE: Presentation,
  WORKSHOP: Briefcase,
}

interface EventImageProps {
  src: string | null
  alt: string
  category?: EventCategory
  className?: string
  eager?: boolean
  width?: number
}

/** Lazy-loaded event artwork with a quiet category placeholder if the image is missing or fails. */
export function EventImage({ src, alt, category, className, eager = false, width = 640 }: EventImageProps) {
  const [failed, setFailed] = useState(false)
  const Icon = category ? CATEGORY_ICONS[category] : Ticket

  if (!src || failed) {
    return (
      <div className={cn('flex items-center justify-center bg-zinc-100', className)} role="img" aria-label={alt}>
        <Icon className="size-8 text-zinc-300" aria-hidden />
      </div>
    )
  }

  return (
    <img
      src={sized(src, width)}
      alt={alt}
      loading={eager ? 'eager' : 'lazy'}
      decoding="async"
      onError={() => setFailed(true)}
      className={cn('bg-zinc-100 object-cover', className)}
    />
  )
}

/** Unsplash URLs accept a width parameter; request only what the layout needs. */
function sized(src: string, width: number) {
  if (!src.includes('images.unsplash.com')) return src
  const url = new URL(src)
  url.searchParams.set('w', String(width))
  return url.toString()
}
