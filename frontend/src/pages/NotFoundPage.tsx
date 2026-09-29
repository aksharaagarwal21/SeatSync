import { SearchX } from 'lucide-react'
import { ButtonLink } from '../components/ui/Button'
import { EmptyState } from '../components/ui/States'

export function NotFoundPage() {
  return (
    <div className="mx-auto max-w-lg pt-10">
      <EmptyState
        icon={SearchX}
        title="Page not found"
        description="The page you’re looking for doesn’t exist or has moved."
        action={<ButtonLink to="/">Browse Events</ButtonLink>}
      />
    </div>
  )
}
