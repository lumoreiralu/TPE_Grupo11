import { useCallback, useEffect, useState } from 'react'
import { Sparkles } from 'lucide-react'
import {
  eliminarPreferencias,
  getPreferencias,
  getRecomendaciones,
  guardarPreferencias,
} from '../api/client'
import { useApp } from '../context/AppContext'
import type { PreferenciasCliente, PreferenciasRequest, Recomendacion } from '../types'
import { EventCard } from './EventCard'
import { EmptyBlock, ErrorBlock, LoadingBlock } from './Feedback'
import { PreferencesPanel } from './PreferencesPanel'

export function RecommendationsSection() {
  const { isAuthenticated } = useApp()
  const [preferencias, setPreferencias] = useState<PreferenciasCliente | null>(null)
  const [recomendaciones, setRecomendaciones] = useState<Recomendacion[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!isAuthenticated) {
      setPreferencias(null)
      setRecomendaciones([])
      setError(null)
      return
    }

    setLoading(true)
    setError(null)
    try {
      const [preferenciasActuales, recomendacionesActuales] = await Promise.all([
        getPreferencias(),
        getRecomendaciones(),
      ])
      setPreferencias(preferenciasActuales)
      setRecomendaciones(recomendacionesActuales)
    } catch (loadError) {
      setError(
        loadError instanceof Error
          ? loadError.message
          : 'No se pudieron cargar tus recomendaciones.',
      )
    } finally {
      setLoading(false)
    }
  }, [isAuthenticated])

  useEffect(() => {
    void load()
  }, [load])

  async function guardar(request: PreferenciasRequest) {
    await guardarPreferencias(request)
    await load()
  }

  async function limpiar() {
    await eliminarPreferencias()
    await load()
  }

  if (!isAuthenticated) {
    return (
      <section className="container recommendations-section">
        <EmptyBlock
          title="Recomendaciones personalizadas"
          description="Iniciá sesión para guardar tus preferencias y recibir eventos sugeridos según tus gustos y compras anteriores."
        />
      </section>
    )
  }

  return (
    <section className="container recommendations-section" aria-labelledby="recomendaciones-titulo">
      <PreferencesPanel
        preferencias={preferencias}
        loading={loading}
        onSave={guardar}
        onClear={limpiar}
      />

      <div className="section-head recommendations-head">
        <div>
          <span className="section-kicker">
            <Sparkles size={15} />
            para vos
          </span>
          <h2 id="recomendaciones-titulo">Recomendados para vos</h2>
        </div>
        <p className="muted">
          {loading
            ? 'Buscando coincidencias…'
            : `${recomendaciones.length} sugerencia${recomendaciones.length === 1 ? '' : 's'}`}
        </p>
      </div>

      {error ? <ErrorBlock message={error} onRetry={() => void load()} /> : null}

      {loading ? <LoadingBlock label="Calculando recomendaciones…" /> : null}

      {!loading && !error && recomendaciones.length === 0 ? (
        <EmptyBlock
          title="Todavía no hay sugerencias para tus gustos"
          description="Guardá tus preferencias o comprá entradas para que podamos sugerirte eventos parecidos a los que te interesan."
        />
      ) : null}

      {!loading && !error && recomendaciones.length > 0 ? (
        <div className="recommend-grid">
          {recomendaciones.map((recomendacion) => (
            <div className="recommend-card" key={recomendacion.evento.id}>
              <span className="recommend-badge">{recomendacion.motivo}</span>
              <EventCard evento={recomendacion.evento} />
            </div>
          ))}
        </div>
      ) : null}
    </section>
  )
}
