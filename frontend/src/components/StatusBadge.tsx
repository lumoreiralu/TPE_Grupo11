import type { EstadoEntrada, EstadoOrden } from '../types'

const ORDER_META: Record<EstadoOrden, { label: string; tone: string }> = {
  RESERVADA_TEMPORAL: { label: 'Reserva temporal', tone: 'warning' },
  PROCESANDO_PAGO: { label: 'Procesando pago', tone: 'info' },
  PAGO_CONFIRMADO: { label: 'Pago confirmado', tone: 'success' },
  COMPLETADA: { label: 'Completada', tone: 'success' },
  EXPIRADA: { label: 'Expirada', tone: 'danger' },
  CANCELADA: { label: 'Cancelada', tone: 'danger' },
}

const TICKET_META: Record<EstadoEntrada, { label: string; tone: string }> = {
  EMITIDA: { label: 'Válida', tone: 'success' },
  USADA: { label: 'Usada', tone: 'neutral' },
  CANCELADA: { label: 'Cancelada', tone: 'danger' },
}

export function OrderStatusBadge({ estado }: { estado?: EstadoOrden | string | null }) {
  const meta = estado
    ? ORDER_META[estado as EstadoOrden] ?? { label: estado, tone: 'neutral' }
    : { label: 'Sin estado', tone: 'neutral' }

  return <span className={`status-badge tone-${meta.tone}`}>{meta.label}</span>
}

export function TicketStatusBadge({ estado }: { estado?: EstadoEntrada | string | null }) {
  const meta = estado
    ? TICKET_META[estado as EstadoEntrada] ?? { label: estado, tone: 'neutral' }
    : { label: 'Sin estado', tone: 'neutral' }

  return <span className={`status-badge tone-${meta.tone}`}>{meta.label}</span>
}
