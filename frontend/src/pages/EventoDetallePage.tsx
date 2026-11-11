import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  Armchair,
  ArrowLeft,
  CalendarDays,
  Clock,
  Landmark,
  MapPin,
  Trash2,
  Users,
} from 'lucide-react'
import { getEvento, getLugar, reservar } from '../api/client'
import { useApp } from '../context/AppContext'
import type { Evento, Lugar, Sector } from '../types'
import type { SavedOrder, SelectedSeat } from '../lib/storage'
import { saveOrder } from '../lib/storage'
import { formatDateTime, formatPrice, seatLabel } from '../lib/format'
import { getEventMeta } from '../lib/eventMeta'
import { SeatMap } from '../components/SeatMap'
import { EmptyBlock, ErrorBlock, LoadingBlock } from '../components/Feedback'

const MAX_ENTRADAS = (() => {
  const configured = Number(import.meta.env.VITE_MAX_ENTRADAS)
  return Number.isFinite(configured) && configured > 0 ? configured : 4
})()

function createIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}

export function EventoDetallePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { clienteId } = useApp()

  const [evento, setEvento] = useState<Evento | null>(null)
  const [lugar, setLugar] = useState<Lugar | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [lugarError, setLugarError] = useState<string | null>(null)

  const [selected, setSelected] = useState<SelectedSeat[]>([])
  const [selectionNotice, setSelectionNotice] = useState<string | null>(null)
  const [reserveError, setReserveError] = useState<string | null>(null)
  const [reserving, setReserving] = useState(false)
  const idempotencyKeyRef = useRef<string | null>(null)

  const load = useCallback(async () => {
    if (!id) return
    setLoading(true)
    setError(null)
    setLugarError(null)
    setSelected([])
    setSelectionNotice(null)
    setReserveError(null)
    idempotencyKeyRef.current = null

    try {
      const loadedEvent = await getEvento(id)
      setEvento(loadedEvent)

      if (loadedEvent.lugarId) {
        try {
          setLugar(await getLugar(loadedEvent.lugarId))
        } catch (venueError) {
          setLugarError(
            errorMessage(
              venueError,
              'No pudimos cargar la configuración del recinto para este evento.',
            ),
          )
        }
      }
    } catch (loadError) {
      setEvento(null)
      setError(errorMessage(loadError, 'No pudimos cargar el evento.'))
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    void load()
  }, [load])

  const priceBySector = useMemo(() => {
    const map = new Map<string, number>()
    for (const precio of evento?.preciosSectores ?? []) {
      map.set(precio.idSector, Number(precio.precio))
    }
    return map
  }, [evento])

  const sectores = useMemo(
    () => (lugar?.sectores ?? []).filter((sector) => priceBySector.has(sector.sectorId)),
    [lugar, priceBySector],
  )

  const minPrice = useMemo(() => {
    const prices = [...priceBySector.values()].filter(
      (precio) => Number.isFinite(precio) && precio > 0,
    )
    return prices.length ? Math.min(...prices) : null
  }, [priceBySector])

  const total = selected.reduce((sum, seat) => sum + seat.precio, 0)
  const meta = getEventMeta(evento?.tipoEvento)
  const TypeIcon = meta.Icon
  const canBuy = Boolean(evento) && (!evento?.estado || evento.estado === 'PUBLICADO')

  function seatKey(sectorId: string, fila: string, asiento: number): string {
    return `${sectorId}|${fila}|${asiento}`
  }

  function toggleSeat(sector: Sector, fila: string, asiento: number, precio: number) {
    const key = seatKey(sector.sectorId, fila, asiento)
    const existing = selected.findIndex(
      (seat) => seatKey(seat.sectorId, seat.fila, seat.asiento) === key,
    )

    if (existing >= 0) {
      setSelected((current) =>
        current.filter(
          (seat) => seatKey(seat.sectorId, seat.fila, seat.asiento) !== key,
        ),
      )
      setSelectionNotice(null)
      idempotencyKeyRef.current = null
      return
    }

    if (selected.length >= MAX_ENTRADAS) {
      setSelectionNotice(`Podés elegir hasta ${MAX_ENTRADAS} entradas por compra.`)
      return
    }

    setSelected((current) => [
      ...current,
      {
        sectorId: sector.sectorId,
        sectorNombre: sector.nombre,
        fila,
        asiento,
        precio,
      },
    ])
    setSelectionNotice(null)
    setReserveError(null)
    idempotencyKeyRef.current = null
  }

  function removeSeat(key: string) {
    setSelected((current) =>
      current.filter((seat) => seatKey(seat.sectorId, seat.fila, seat.asiento) !== key),
    )
    setSelectionNotice(null)
    setReserveError(null)
    idempotencyKeyRef.current = null
  }

  async function handleReserve() {
    if (!evento || selected.length === 0 || !canBuy) return

    setReserving(true)
    setReserveError(null)

    try {
      if (!idempotencyKeyRef.current) {
        idempotencyKeyRef.current = createIdempotencyKey()
      }

      const response = await reservar(
        {
          clienteId,
          eventoId: evento.id,
          items: selected.map(({ sectorId, fila, asiento }) => ({
            sectorId,
            fila,
            asiento,
          })),
        },
        idempotencyKeyRef.current,
      )

      const order: SavedOrder = {
        ordenId: response.ordenId,
        eventoId: evento.id,
        eventoNombre: evento.nombre,
        eventoFecha: evento.fechaHora,
        lugarNombre: lugar?.nombre ?? '',
        ciudad: evento.ciudad ?? lugar?.ciudad ?? '',
        items: selected,
        montoTotal: Number(response.montoTotal),
        estado: response.estado,
        expiraEn: response.expiraEn,
        createdAt: new Date().toISOString(),
      }

      saveOrder(order)
      navigate(`/checkout/${response.ordenId}`, { state: { order } })
    } catch (reserveFailure) {
      setReserveError(
        errorMessage(
          reserveFailure,
          'No pudimos reservar los asientos. Intentá nuevamente.',
        ),
      )
    } finally {
      setReserving(false)
    }
  }

  if (loading) {
    return (
      <div className="page">
        <div className="container page-pad">
          <LoadingBlock label="Cargando evento y recinto…" />
        </div>
      </div>
    )
  }

  if (error || !evento) {
    return (
      <div className="page">
        <div className="container page-pad">
          <Link to="/" className="back-link">
            <ArrowLeft size={16} />
            Volver a eventos
          </Link>
          <ErrorBlock
            message={error ?? 'El evento no existe o no está disponible.'}
            onRetry={() => void load()}
          />
        </div>
      </div>
    )
  }

  return (
    <div className="page detail-page">
      <div className="container">
        <Link to="/" className="back-link">
          <ArrowLeft size={16} />
          Volver a eventos
        </Link>

        <section className="detail-hero" style={{ background: meta.gradient }}>
          <TypeIcon className="detail-hero-icon" size={180} strokeWidth={0.9} aria-hidden="true" />
          <div className="detail-hero-inner">
            <div className="detail-hero-copy">
              <span className="hero-event-type">
                <TypeIcon size={15} />
                {meta.label}
              </span>
              <h1>{evento.nombre}</h1>
              {evento.descripcion ? <p>{evento.descripcion}</p> : null}

              <div className="detail-meta">
                <span>
                  <CalendarDays size={16} />
                  {formatDateTime(evento.fechaHora)}
                </span>
                {lugar?.nombre ? (
                  <span>
                    <Landmark size={16} />
                    {lugar.nombre}
                    {lugar.direccion ? ` · ${lugar.direccion}` : ''}
                  </span>
                ) : null}
                {evento.ciudad || lugar?.ciudad ? (
                  <span>
                    <MapPin size={16} />
                    {evento.ciudad ?? lugar?.ciudad}
                  </span>
                ) : null}
              </div>

              {evento.participantes?.length ? (
                <div className="participant-list">
                  <Users size={16} />
                  {evento.participantes.map((participante) => (
                    <span key={participante}>{participante}</span>
                  ))}
                </div>
              ) : null}
            </div>

            <div className="detail-hero-aside">
              <span>Entradas desde</span>
              <strong>{formatPrice(minPrice)}</strong>
              <span className="detail-hero-hint">Elegí sector y asiento</span>
            </div>
          </div>
        </section>

        {!canBuy ? (
          <div className="notice banner">
            <Clock size={17} />
            <p>Este evento todavía no está publicado, por lo que no se pueden reservar entradas.</p>
          </div>
        ) : null}

        <div className="detail-layout">
          <div className="detail-main">
            <div className="section-head compact">
              <div>
                <span className="section-kicker">
                  <Armchair size={15} />
                  Sectores y asientos
                </span>
                <h2>Elegí tus asientos</h2>
              </div>
              <p className="muted">Hasta {MAX_ENTRADAS} entradas por compra</p>
            </div>

            {lugarError ? <ErrorBlock message={lugarError} onRetry={() => void load()} /> : null}

            {!lugarError && sectores.length === 0 ? (
              <EmptyBlock
                title="El recinto no tiene sectores con precio"
                description="El evento tiene que incluir precios por sector y el lugar debe tener los sectores configurados para poder reservar."
                action={
                  <Link to="/" className="btn btn-primary">
                    Ver otros eventos
                  </Link>
                }
              />
            ) : null}

            {!lugarError
              ? sectores.map((sector) => (
                  <SeatMap
                    key={sector.sectorId}
                    sector={sector}
                    precio={priceBySector.get(sector.sectorId) ?? 0}
                    selected={selected}
                    maxEntradas={MAX_ENTRADAS}
                    onToggle={toggleSeat}
                  />
                ))
              : null}
          </div>

          <aside className="selection-panel">
            <div className="selection-panel-head">
              <h2>Tu selección</h2>
              <span className="selection-count">{selected.length}</span>
            </div>

            {selected.length === 0 ? (
              <p className="muted selection-empty">
                Todavía no elegiste asientos. Hacé clic en un asiento disponible para agregarlo.
              </p>
            ) : (
              <ul className="selection-list">
                {selected.map((seat) => {
                  const key = seatKey(seat.sectorId, seat.fila, seat.asiento)
                  return (
                    <li key={key}>
                      <div className="selection-item-info">
                        <strong>{seat.sectorNombre}</strong>
                        <span>{seatLabel(seat.fila, seat.asiento)}</span>
                      </div>
                      <span className="selection-item-price">{formatPrice(seat.precio)}</span>
                      <button
                        type="button"
                        className="icon-button small"
                        onClick={() => removeSeat(key)}
                        aria-label={`Quitar ${seatLabel(seat.fila, seat.asiento)}`}
                      >
                        <Trash2 size={15} />
                      </button>
                    </li>
                  )
                })}
              </ul>
            )}

            <div className="selection-total">
              <span>Total</span>
              <strong>{formatPrice(total)}</strong>
            </div>

            <button
              type="button"
              className="btn btn-primary btn-block"
              disabled={selected.length === 0 || reserving || !canBuy}
              onClick={() => void handleReserve()}
            >
              {reserving ? 'Reservando…' : 'Reservar asientos'}
            </button>

            <p className="hint">
              <Clock size={14} />
              Al reservar, los asientos quedan bloqueados durante 10 minutos. Después elegís el
              medio de pago.
            </p>

            {selectionNotice ? <p className="form-note warning">{selectionNotice}</p> : null}
            {reserveError ? <p className="form-note error">{reserveError}</p> : null}
          </aside>
        </div>
      </div>
    </div>
  )
}
