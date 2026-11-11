import { Link } from 'react-router-dom'
import { ArrowRight, CalendarDays, MapPin, Users } from 'lucide-react'
import type { Evento } from '../types'
import { formatDateTime, formatPrice } from '../lib/format'
import { getEventMeta } from '../lib/eventMeta'

export function EventCard({ evento }: { evento: Evento }) {
  const meta = getEventMeta(evento.tipoEvento)
  const Icon = meta.Icon

  const prices = (evento.preciosSectores ?? [])
    .map((precio) => Number(precio.precio))
    .filter((precio) => Number.isFinite(precio) && precio > 0)
  const minPrice = prices.length ? Math.min(...prices) : null
  const participants = evento.participantes ?? []

  return (
    <Link
      to={`/eventos/${evento.id}`}
      className="event-card"
      aria-label={`Ver entradas para ${evento.nombre}`}
    >
      <div className="event-card-art" style={{ background: meta.gradient }}>
        <span className="event-type-pill">
          <Icon size={14} />
          {meta.label}
        </span>
        <Icon className="event-art-icon" size={88} strokeWidth={1.15} aria-hidden="true" />
        <span className="event-art-glow" aria-hidden="true" />
      </div>

      <div className="event-card-body">
        <h3 className="event-card-title">{evento.nombre}</h3>

        <div className="meta-list">
          <span>
            <CalendarDays size={15} />
            {formatDateTime(evento.fechaHora)}
          </span>
          <span>
            <MapPin size={15} />
            {evento.ciudad || 'Ciudad a confirmar'}
          </span>
          {participants.length ? (
            <span>
              <Users size={15} />
              {participants.slice(0, 2).join(' · ')}
              {participants.length > 2 ? ` +${participants.length - 2}` : ''}
            </span>
          ) : null}
        </div>

        <div className="event-card-footer">
          <div className="event-price">
            <span>Desde</span>
            <strong>{formatPrice(minPrice)}</strong>
          </div>
          <span className="card-cta">
            Ver entradas
            <ArrowRight size={16} />
          </span>
        </div>
      </div>
    </Link>
  )
}
