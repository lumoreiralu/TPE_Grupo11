import { useCallback, useEffect, useState } from 'react'
import { CalendarDays, QrCode, ShieldCheck, Sparkles, Timer } from 'lucide-react'
import { buscarEventos } from '../api/client'
import type { BusquedaEventosParams, Evento } from '../types'
import { SearchPanel } from '../components/SearchPanel'
import { EventCard } from '../components/EventCard'
import { EmptyBlock, ErrorBlock, LoadingBlock } from '../components/Feedback'

export function EventosPage() {
  const [filters, setFilters] = useState<BusquedaEventosParams>({})
  const [eventos, setEventos] = useState<Evento[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [hasSearched, setHasSearched] = useState(false)

  const load = useCallback(async (params: BusquedaEventosParams) => {
    setLoading(true)
    setError(null)
    setHasSearched(Object.values(params).some((value) => Boolean(value)))
    try {
      setEventos(await buscarEventos(params))
    } catch (loadError) {
      setEventos([])
      setError(
        loadError instanceof Error
          ? loadError.message
          : 'No se pudieron cargar los eventos.',
      )
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load({})
  }, [load])

  function resetSearch() {
    setFilters({})
    void load({})
  }

  return (
    <div className="page">
      <section className="hero">
        <div className="container hero-inner">
          <div className="hero-copy">
            <span className="eyebrow">
              <Sparkles size={15} />
              Entradas para lo que te gusta
            </span>
            <h1>
              Tu próxima gran noche
              <span> empieza acá.</span>
            </h1>
            <p className="hero-lead">
              Buscá eventos, elegí tu sector y asiento, y comprá en minutos. Tus entradas
              quedan con un código QR único listo para el acceso.
            </p>
            <div className="hero-pills">
              <span>
                <Timer size={15} />
                Reserva de 10 minutos
              </span>
              <span>
                <QrCode size={15} />
                QR único por entrada
              </span>
              <span>
                <ShieldCheck size={15} />
                Compra protegida
              </span>
            </div>
          </div>

          <div className="hero-search">
            <SearchPanel
              filters={filters}
              loading={loading}
              onChange={setFilters}
              onSubmit={() => void load(filters)}
              onReset={resetSearch}
            />
          </div>
        </div>
      </section>

      <section className="container results-section">
        <div className="section-head">
          <div>
            <span className="section-kicker">
              <CalendarDays size={15} />
              {hasSearched ? 'Búsqueda' : 'Cartelera'}
            </span>
            <h2>{hasSearched ? 'Resultados de búsqueda' : 'Próximos eventos'}</h2>
          </div>
          <p className="muted">
            {loading
              ? 'Buscando eventos…'
              : `${eventos.length} evento${eventos.length === 1 ? '' : 's'} encontrado${
                  eventos.length === 1 ? '' : 's'
                }`}
          </p>
        </div>

        {error ? <ErrorBlock message={error} onRetry={() => void load(filters)} /> : null}

        {loading ? <LoadingBlock label="Cargando cartelera…" /> : null}

        {!loading && !error && eventos.length === 0 ? (
          <EmptyBlock
            title={hasSearched ? 'No encontramos eventos con esos filtros' : 'Todavía no hay eventos publicados'}
            description={
              hasSearched
                ? 'Probá con otro artista, ciudad o rango de fechas.'
                : 'Cuando se publiquen eventos van a aparecer en esta cartelera. También podés cargar datos de prueba desde la API y recargar la página.'
            }
            action={
              hasSearched ? (
                <button type="button" className="btn btn-primary" onClick={resetSearch}>
                  Ver todos los eventos
                </button>
              ) : undefined
            }
          />
        ) : null}

        {!loading && !error && eventos.length > 0 ? (
          <div className="event-grid">
            {eventos.map((evento) => (
              <EventCard key={evento.id} evento={evento} />
            ))}
          </div>
        ) : null}
      </section>
    </div>
  )
}
