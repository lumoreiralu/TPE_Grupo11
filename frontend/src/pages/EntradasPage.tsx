import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type FormEvent,
} from 'react'
import { Link } from 'react-router-dom'
import { AlertCircle, Search, Ticket } from 'lucide-react'
import {
  getEntradasPorCliente,
  getEntradasPorOrden,
  getEvento,
  getLugar,
} from '../api/client'
import type { Entrada, Evento, Lugar } from '../types'
import { useApp } from '../context/AppContext'
import { getOrders } from '../lib/storage'
import { getErrorMessage } from '../lib/errors'
import { TicketCard } from '../components/TicketCard'
import { EmptyBlock, ErrorBlock, LoadingBlock } from '../components/Feedback'

function mergeTickets(...groups: Entrada[][]): Entrada[] {
  const map = new Map<string, Entrada>()
  for (const group of groups) {
    for (const ticket of group) {
      map.set(ticket.id, ticket)
    }
  }
  return [...map.values()].sort((a, b) =>
    a.fechaEmision < b.fechaEmision ? 1 : -1,
  )
}

export function EntradasPage() {
  const { clienteId } = useApp()

  const [tickets, setTickets] = useState<Entrada[]>([])
  const [events, setEvents] = useState<Record<string, Evento>>({})
  const [venues, setVenues] = useState<Record<string, Lugar>>({})
  const [sectorNames, setSectorNames] = useState<Record<string, Record<string, string>>>({})
  const [orders, setOrders] = useState(() => getOrders())
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [lookupId, setLookupId] = useState('')
  const [lookupLoading, setLookupLoading] = useState(false)
  const [lookupError, setLookupError] = useState<string | null>(null)

  const loadEvents = useCallback(async (list: Entrada[]) => {
    const eventIds = [...new Set(list.map((ticket) => ticket.eventoId).filter(Boolean))]
    const results = await Promise.all(
      eventIds.map(async (eventId) => {
        try {
          return await getEvento(eventId)
        } catch {
          return null
        }
      }),
    )

    const eventMap: Record<string, Evento> = {}
    for (const event of results) {
      if (event) eventMap[event.id] = event
    }
    setEvents(eventMap)

    const venueIds = [
      ...new Set(Object.values(eventMap).map((event) => event.lugarId).filter(Boolean)),
    ]
    const venueResults = await Promise.all(
      venueIds.map(async (venueId) => {
        try {
          return await getLugar(venueId)
        } catch {
          return null
        }
      }),
    )

    const venueMap: Record<string, Lugar> = {}
    for (const venue of venueResults) {
      if (venue) venueMap[venue.id] = venue
    }
    setVenues(venueMap)

    const namesByEvent: Record<string, Record<string, string>> = {}
    for (const event of Object.values(eventMap)) {
      const venue = venueMap[event.lugarId]
      if (!venue?.sectores) continue
      namesByEvent[event.id] = Object.fromEntries(
        venue.sectores.map((sector) => [sector.sectorId, sector.nombre]),
      )
    }
    setSectorNames(namesByEvent)
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)

    const localOrders = getOrders()
    setOrders(localOrders)
    const localTickets = localOrders.flatMap((order) => order.entradas ?? [])

    try {
      const remoteTickets = await getEntradasPorCliente(clienteId)
      const merged = mergeTickets(remoteTickets, localTickets)
      setTickets(merged)
      await loadEvents(merged)
    } catch (loadFailure) {
      const fallback = mergeTickets(localTickets)
      setTickets(fallback)
      setError(
        getErrorMessage(
          loadFailure,
          'No pudimos consultar el servicio de entradas. Mostrando las entradas guardadas en este dispositivo.',
        ),
      )
      await loadEvents(fallback)
    } finally {
      setLoading(false)
    }
  }, [clienteId, loadEvents])

  useEffect(() => {
    void load()
  }, [load])

  const ordersById = useMemo(
    () => new Map(orders.map((order) => [order.ordenId, order])),
    [orders],
  )

  async function handleLookup(event: FormEvent) {
    event.preventDefault()
    const orderId = lookupId.trim()
    if (!orderId) return

    setLookupLoading(true)
    setLookupError(null)

    try {
      const found = await getEntradasPorOrden(orderId)
      if (found.length === 0) {
        setLookupError('No encontramos entradas emitidas para esa orden.')
        return
      }

      const merged = mergeTickets(tickets, found)
      setTickets(merged)
      await loadEvents(merged)
      setLookupId('')
    } catch (lookupFailure) {
      setLookupError(
        getErrorMessage(
          lookupFailure,
          'No pudimos buscar la orden. Revisá el ID e intentá nuevamente.',
        ),
      )
    } finally {
      setLookupLoading(false)
    }
  }

  return (
    <div className="page">
      <div className="container page-pad">
        <div className="tickets-head">
          <span className="section-kicker">
            <Ticket size={15} />
            Mis entradas
          </span>
          <h1>Tus entradas</h1>
          <p className="hero-lead">
            Acá están las entradas emitidas para el cliente <strong>{clienteId}</strong>. Presentá
            el código QR en el acceso del evento.
          </p>
        </div>

        <div className="card tickets-toolbar">
          <div>
            <strong>Buscar una orden</strong>
            <p className="muted small">
              Si tenés el ID de una orden, podés traer sus entradas a esta lista.
            </p>
          </div>
          <form className="lookup-form" onSubmit={handleLookup}>
            <input
              className="input"
              value={lookupId}
              onChange={(event) => setLookupId(event.target.value)}
              placeholder="Ej. f3f199c2-5b80-4a2e-834f-e6e0bf97dabb"
              aria-label="ID de orden"
            />
            <button
              type="submit"
              className="btn btn-primary"
              disabled={lookupLoading || !lookupId.trim()}
            >
              <Search size={16} />
              {lookupLoading ? 'Buscando…' : 'Buscar'}
            </button>
          </form>
        </div>

        {lookupError ? <p className="form-note error">{lookupError}</p> : null}

        {loading ? <LoadingBlock label="Cargando tus entradas…" /> : null}

        {error && !loading ? (
          tickets.length === 0 ? (
            <ErrorBlock message={error} onRetry={() => void load()} />
          ) : (
            <div className="notice banner">
              <AlertCircle size={17} />
              <p>{error}</p>
            </div>
          )
        ) : null}

        {!loading && tickets.length === 0 ? (
          <EmptyBlock
            title="Todavía no tenés entradas"
            description="Cuando confirmes una compra, tus entradas con código QR van a aparecer acá."
            action={
              <Link to="/" className="btn btn-primary">
                Buscar eventos
              </Link>
            }
          />
        ) : null}

        {!loading && tickets.length > 0 ? (
          <section className="tickets-section">
            <div className="section-head compact">
              <div>
                <span className="section-kicker">Entradas emitidas</span>
                <h2>{tickets.length} entrada{tickets.length === 1 ? '' : 's'}</h2>
              </div>
            </div>
            <div className="ticket-list">
              {tickets.map((ticket) => {
                const event = events[ticket.eventoId] ?? null
                return (
                  <TicketCard
                    key={ticket.id}
                    ticket={ticket}
                    event={event}
                    order={ordersById.get(ticket.ordenCompraId) ?? null}
                    venueName={event ? venues[event.lugarId]?.nombre : undefined}
                    sectorNames={sectorNames[ticket.eventoId]}
                  />
                )
              })}
            </div>
          </section>
        ) : null}
      </div>
    </div>
  )
}
