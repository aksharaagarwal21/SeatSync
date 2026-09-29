import { useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { adminApi, queryKeys } from '../../api/queries'
import { Button } from '../../components/ui/Button'
import { SelectField, TextField } from '../../components/ui/Field'
import { Skeleton } from '../../components/ui/Skeleton'
import { ErrorState } from '../../components/ui/States'
import { useToast } from '../../context/ToastContext'
import { ApiError, errorMessage } from '../../lib/api'
import { CATEGORY_LABELS, formatPrice, toIsoDate } from '../../lib/format'
import type { AdminEvent, EventCategory, EventRequest } from '../../lib/types'

type FormErrors = Record<string, string>

const EMPTY_FORM: EventRequest = {
  name: '',
  description: '',
  category: 'CONCERT',
  venue: '',
  city: '',
  eventDate: '',
  startTime: '19:00',
  imageUrl: '',
  bookingOpen: true,
  rows: 10,
  seatsPerRow: 20,
  pricing: { vip: 1999, premium: 999, standard: 499 },
}

export function AdminEventFormPage() {
  const { eventId: eventParam } = useParams()
  const eventId = eventParam ? Number(eventParam) : null
  const { data: existing, isPending, isError, error, refetch } = useQuery({
    queryKey: queryKeys.adminEvent(eventId ?? 0),
    queryFn: () => adminApi.event(eventId!),
    enabled: eventId !== null,
  })

  if (eventId !== null && isPending) {
    return (
      <div className="max-w-2xl space-y-4" aria-busy>
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-96 w-full rounded-xl" />
      </div>
    )
  }
  if (eventId !== null && isError) return <ErrorState message={errorMessage(error)} onRetry={() => refetch()} />

  return <EventForm key={eventId ?? 'new'} existing={existing} />
}

function toForm(event: AdminEvent): EventRequest {
  return {
    name: event.name,
    description: event.description,
    category: event.category,
    venue: event.venue,
    city: event.city,
    eventDate: event.eventDate,
    startTime: event.startTime.slice(0, 5),
    imageUrl: event.imageUrl ?? '',
    bookingOpen: event.status === 'ON_SALE',
    rows: event.rows,
    seatsPerRow: event.seatsPerRow,
    pricing: event.pricing,
  }
}

function validate(form: EventRequest, isNew: boolean): FormErrors {
  const errors: FormErrors = {}
  if (!form.name.trim()) errors.name = 'Event name is required'
  if (!form.description.trim()) errors.description = 'Description is required'
  if (!form.venue.trim()) errors.venue = 'Venue is required'
  if (!form.city.trim()) errors.city = 'City is required'
  if (!form.eventDate) errors.eventDate = 'Date is required'
  else if (isNew && form.eventDate < toIsoDate(new Date())) errors.eventDate = 'Date can’t be in the past'
  if (!form.startTime) errors.startTime = 'Start time is required'
  if (form.imageUrl && !/^https?:\/\/\S+$/.test(form.imageUrl)) errors.imageUrl = 'Use a full http(s) URL'
  if (!(form.rows >= 1 && form.rows <= 26)) errors.rows = '1–26 rows'
  if (!(form.seatsPerRow >= 1 && form.seatsPerRow <= 40)) errors.seatsPerRow = '1–40 seats'
  ;(['vip', 'premium', 'standard'] as const).forEach((tier) => {
    if (!(form.pricing[tier] > 0)) errors[`pricing.${tier}`] = 'Enter a price'
  })
  return errors
}

function EventForm({ existing }: { existing?: AdminEvent }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const toast = useToast()
  const isNew = !existing
  const [form, setForm] = useState<EventRequest>(existing ? toForm(existing) : EMPTY_FORM)
  const [errors, setErrors] = useState<FormErrors>({})
  const layoutLocked = !!existing && existing.bookedSeats + existing.heldSeats > 0

  const save = useMutation({
    mutationFn: (request: EventRequest) => (existing ? adminApi.updateEvent(existing.id, request) : adminApi.createEvent(request)),
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['admin'] })
      queryClient.invalidateQueries({ queryKey: ['events'] })
      queryClient.invalidateQueries({ queryKey: queryKeys.event(saved.id) })
      toast.success(isNew ? `${saved.name} was created.` : `${saved.name} was updated.`)
      navigate('/admin/events')
    },
    onError: (err) => {
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length) setErrors(err.fieldErrors)
      else toast.warning(errorMessage(err))
    },
  })

  const set = <K extends keyof EventRequest>(key: K, value: EventRequest[K]) => setForm((current) => ({ ...current, [key]: value }))
  const setPrice = (tier: keyof EventRequest['pricing'], value: string) =>
    setForm((current) => ({ ...current, pricing: { ...current.pricing, [tier]: Number(value) } }))

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    const validation = validate(form, isNew)
    setErrors(validation)
    if (Object.keys(validation).length === 0) {
      save.mutate({ ...form, imageUrl: form.imageUrl.trim() })
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="max-w-2xl">
      <Link to="/admin/events" className="mb-4 inline-flex items-center gap-1.5 rounded-md text-sm text-zinc-500 hover:text-zinc-900">
        <ArrowLeft className="size-4" aria-hidden /> Events
      </Link>
      <h1 className="mb-6 text-2xl font-semibold tracking-tight">{isNew ? 'New event' : `Edit ${existing.name}`}</h1>

      <div className="space-y-5">
        <FormSection title="Details">
          <TextField label="Event name" value={form.name} onChange={(e) => set('name', e.target.value)} error={errors.name} />
          <div>
            <label htmlFor="description" className="label">
              Description
            </label>
            <textarea
              id="description"
              rows={3}
              className="input h-auto py-2"
              value={form.description}
              onChange={(e) => set('description', e.target.value)}
              aria-invalid={errors.description ? true : undefined}
            />
            {errors.description && <p className="mt-1.5 text-[13px] text-red-600">{errors.description}</p>}
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <SelectField label="Category" value={form.category} onChange={(e) => set('category', e.target.value as EventCategory)}>
              {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </SelectField>
            <TextField
              label="Image URL"
              placeholder="https://…"
              value={form.imageUrl}
              onChange={(e) => set('imageUrl', e.target.value)}
              error={errors.imageUrl}
            />
          </div>
        </FormSection>

        <FormSection title="Venue & schedule">
          <div className="grid gap-4 sm:grid-cols-2">
            <TextField label="Venue" value={form.venue} onChange={(e) => set('venue', e.target.value)} error={errors.venue} />
            <TextField label="City" value={form.city} onChange={(e) => set('city', e.target.value)} error={errors.city} />
            <TextField label="Date" type="date" value={form.eventDate} onChange={(e) => set('eventDate', e.target.value)} error={errors.eventDate} />
            <TextField label="Start time" type="time" value={form.startTime} onChange={(e) => set('startTime', e.target.value)} error={errors.startTime} />
          </div>
        </FormSection>

        <FormSection
          title="Seating & pricing"
          description={
            layoutLocked
              ? 'The seat layout is locked because seats have been booked. Prices can still change for unsold seats.'
              : 'Front rows are VIP, the middle rows Premium and the back rows Standard.'
          }
        >
          <div className="grid grid-cols-2 gap-4">
            <TextField
              label="Rows"
              type="number"
              min={1}
              max={26}
              value={form.rows}
              disabled={layoutLocked}
              onChange={(e) => set('rows', Number(e.target.value))}
              error={errors.rows}
            />
            <TextField
              label="Seats per row"
              type="number"
              min={1}
              max={40}
              value={form.seatsPerRow}
              disabled={layoutLocked}
              onChange={(e) => set('seatsPerRow', Number(e.target.value))}
              error={errors.seatsPerRow}
            />
          </div>
          <p className="text-[13px] text-zinc-500">
            {form.rows * form.seatsPerRow || 0} seats · capacity revenue up to{' '}
            {formatPrice(estimateCapacity(form))}
          </p>
          <div className="grid gap-4 sm:grid-cols-3">
            {(['vip', 'premium', 'standard'] as const).map((tier) => (
              <TextField
                key={tier}
                label={`${tier === 'vip' ? 'VIP' : tier[0].toUpperCase() + tier.slice(1)} price (₹)`}
                type="number"
                min={1}
                value={form.pricing[tier] || ''}
                onChange={(e) => setPrice(tier, e.target.value)}
                error={errors[`pricing.${tier}`]}
              />
            ))}
          </div>
        </FormSection>

        <FormSection title="Booking">
          <label className="flex cursor-pointer items-start gap-3">
            <input
              type="checkbox"
              className="mt-0.5 size-4 rounded border-zinc-300 accent-brand-700"
              checked={form.bookingOpen}
              onChange={(e) => set('bookingOpen', e.target.checked)}
            />
            <span>
              <span className="block text-sm font-medium">Booking open</span>
              <span className="block text-[13px] text-zinc-500">When off, the event stays visible but seats can’t be booked.</span>
            </span>
          </label>
        </FormSection>
      </div>

      <div className="mt-6 flex justify-end gap-2">
        <Button variant="secondary" onClick={() => navigate('/admin/events')}>
          Cancel
        </Button>
        <Button type="submit" loading={save.isPending}>
          {isNew ? 'Create event' : 'Save changes'}
        </Button>
      </div>
    </form>
  )
}

function FormSection({ title, description, children }: { title: string; description?: string; children: ReactNode }) {
  return (
    <fieldset className="card space-y-4 p-5">
      <legend className="sr-only">{title}</legend>
      <div>
        <h2 className="text-base font-semibold" aria-hidden>
          {title}
        </h2>
        {description && <p className="mt-0.5 text-[13px] text-zinc-500">{description}</p>}
      </div>
      {children}
    </fieldset>
  )
}

/** Mirrors the backend's VIP 20% / Premium 40% / Standard 40% row split. */
function estimateCapacity(form: EventRequest) {
  const rows = Math.max(0, form.rows || 0)
  const vipRows = Math.max(1, Math.round(rows * 0.2))
  const premiumRows = Math.round(rows * 0.4)
  const standardRows = Math.max(0, rows - vipRows - premiumRows)
  const perRow = form.seatsPerRow || 0
  return perRow * (vipRows * form.pricing.vip + premiumRows * form.pricing.premium + standardRows * form.pricing.standard) || 0
}
