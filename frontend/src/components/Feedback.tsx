import type { ReactNode } from 'react'
import { AlertCircle, Inbox, Loader2, RefreshCw } from 'lucide-react'

export function LoadingBlock({ label = 'Cargando…' }: { label?: string }) {
  return (
    <div className="feedback loading-block" role="status" aria-live="polite">
      <Loader2 size={28} className="spin" />
      <p>{label}</p>
    </div>
  )
}

export function ErrorBlock({
  message,
  onRetry,
}: {
  message: string
  onRetry?: () => void
}) {
  return (
    <div className="feedback error-block" role="alert">
      <AlertCircle size={28} />
      <div>
        <strong>No pudimos completar la operación</strong>
        <p>{message}</p>
      </div>
      {onRetry ? (
        <button type="button" className="btn btn-ghost btn-compact" onClick={onRetry}>
          <RefreshCw size={15} />
          Reintentar
        </button>
      ) : null}
    </div>
  )
}

export function EmptyBlock({
  title,
  description,
  action,
}: {
  title: string
  description: string
  action?: ReactNode
}) {
  return (
    <div className="feedback empty-block">
      <Inbox size={34} />
      <h2>{title}</h2>
      <p>{description}</p>
      {action}
    </div>
  )
}
