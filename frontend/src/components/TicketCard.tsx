import { useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'
import { CalendarDays, Check, Copy, MapPin, Ticket as TicketIcon } from 'lucide-react'
import type { Entrada, Evento } from '../types'
import type { SavedOrder } from '../lib/storage'
import { formatDateTime, formatShortId, seatLabel } from '../lib/format'
import { TicketStatusBadge } from './StatusBadge'

interface TicketCardProps {
  ticket: Entrada
  event?: Evento | null
  order?: SavedOrder | null
  venueName?: string
  sectorNames?: Record<string, string>
}

export function TicketCard({
  ticket,
  event,
  order,
  venueName,
  sectorNames,
}: TicketCardProps) {
  const [copied, setCopied] = useState(false)

  const eventName = event?.nombre ?? order?.eventoNombre ?? 'Evento'
  const eventDate = event?.fechaHora ?? order?.eventoFecha
  const venue = order?.lugarNombre || venueName
  const city = event?.ciudad ?? order?.ciudad
  const matchingItem = order?.items.find(
    (item) =>
      item.sectorId === ticket.sectorNombre &&
      item.fila === ticket.fila &&
      item.asiento === ticket.asiento,
  )
  const sectorName =
    matchingItem?.sectorNombre ?? sectorNames?.[ticket.sectorNombre] ?? ticket.sectorNombre

  async function copyCode() {
    try {
      await navigator.clipboard.writeText(ticket.codigoQrHash)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 2000)
    } catch {
      // El navegador puede bloquear el acceso al portapapeles.
    }
  }

  return (
    <article className="ticket-card">
      <div className="ticket-main">
        <div className="ticket-top">
          <span className="ticket-kicker">
            <TicketIcon size={14} />
            Entrada · {formatShortId(ticket.ordenCompraId)}
          </span>
          <TicketStatusBadge estado={ticket.estado} />
        </div>

        <h3>{eventName}</h3>

        <div className="ticket-meta">
          <span>
            <CalendarDays size={15} />
            {formatDateTime(eventDate)}
          </span>
          {(venue || city) && (
            <span>
              <MapPin size={15} />
              {[venue, city].filter(Boolean).join(' · ')}
            </span>
          )}
        </div>

        <div className="ticket-seat-grid">
          <div>
            <span>Sector</span>
            <strong>{sectorName}</strong>
          </div>
          <div>
            <span>Ubicación</span>
            <strong>{seatLabel(ticket.fila, ticket.asiento)}</strong>
          </div>
        </div>

        <div className="ticket-code">
          <code title={ticket.codigoQrHash}>{ticket.codigoQrHash}</code>
          <button type="button" className="btn btn-ghost btn-compact" onClick={copyCode}>
            {copied ? <Check size={15} /> : <Copy size={15} />}
            {copied ? 'Copiado' : 'Copiar código'}
          </button>
        </div>
      </div>

      <div className="ticket-qr">
        <div className="ticket-qr-frame">
          <QRCodeSVG
            value={ticket.codigoQrHash}
            size={132}
            level="M"
            bgColor="#ffffff"
            fgColor="#17122b"
            title={`Código QR de la entrada ${ticket.id}`}
          />
        </div>
        <span>Escaneá en el acceso</span>
      </div>
    </article>
  )
}
