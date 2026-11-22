import { useEffect, useState, type FormEvent } from 'react'
import { Heart, MapPin, Save, Trash2 } from 'lucide-react'
import type { PreferenciasCliente, PreferenciasRequest, TipoEvento } from '../types'
import { EVENT_META } from '../lib/eventMeta'

interface PreferencesPanelProps {
  preferencias: PreferenciasCliente | null
  loading: boolean
  onSave: (request: PreferenciasRequest) => Promise<void>
  onClear: () => Promise<void>
}

const TIPOS: TipoEvento[] = ['RECITAL', 'TEATRO', 'DEPORTE', 'CONFERENCIA']

function separarArtistas(valor: string): string[] {
  const vistos = new Set<string>()
  return valor
    .split(',')
    .map((artista) => artista.trim())
    .filter((artista) => {
      if (!artista) return false
      const clave = artista.toLocaleLowerCase()
      if (vistos.has(clave)) return false
      vistos.add(clave)
      return true
    })
}

export function PreferencesPanel({
  preferencias,
  loading,
  onSave,
  onClear,
}: PreferencesPanelProps) {
  const [tipos, setTipos] = useState<TipoEvento[]>([])
  const [artistas, setArtistas] = useState('')
  const [ciudad, setCiudad] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setTipos(preferencias?.tiposEvento ?? [])
    setArtistas((preferencias?.artistasFavoritos ?? []).join(', '))
    setCiudad(preferencias?.ciudad ?? '')
  }, [preferencias])

  function alternarTipo(tipo: TipoEvento) {
    setTipos((actuales) =>
      actuales.includes(tipo)
        ? actuales.filter((item) => item !== tipo)
        : [...actuales, tipo],
    )
  }

  async function guardar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await onSave({
        tiposEvento: tipos,
        artistasFavoritos: separarArtistas(artistas),
        ciudad: ciudad.trim() || null,
      })
    } catch (saveError) {
      setError(
        saveError instanceof Error
          ? saveError.message
          : 'No se pudieron guardar tus preferencias.',
      )
    } finally {
      setSaving(false)
    }
  }

  async function limpiar() {
    setSaving(true)
    setError(null)
    try {
      await onClear()
      setTipos([])
      setArtistas('')
      setCiudad('')
    } catch (clearError) {
      setError(
        clearError instanceof Error
          ? clearError.message
          : 'No se pudieron borrar tus preferencias.',
      )
    } finally {
      setSaving(false)
    }
  }

  const tieneDatos =
    tipos.length > 0 || artistas.trim().length > 0 || ciudad.trim().length > 0

  return (
    <section className="preferences-panel" aria-labelledby="preferencias-titulo">
      <div className="preferences-head">
        <div>
          <span className="section-kicker">
            <Heart size={15} />
            tus gustos
          </span>
          <h2 id="preferencias-titulo">Personalizá tus recomendaciones</h2>
        </div>
        <p className="muted small">
          Elegí tipos de evento, artistas y una ciudad. También usamos tu historial de compras.
        </p>
      </div>

      <form onSubmit={(evento) => void guardar(evento)}>
        <div className="preference-types" role="group" aria-label="Tipos de evento favoritos">
          {TIPOS.map((tipo) => {
            const meta = EVENT_META[tipo]
            const Icon = meta.Icon
            const activo = tipos.includes(tipo)
            return (
              <button
                key={tipo}
                type="button"
                className={`type-chip${activo ? ' active' : ''}`}
                aria-pressed={activo}
                onClick={() => alternarTipo(tipo)}
              >
                <Icon size={15} />
                {meta.label}
              </button>
            )
          })}
        </div>

        <div className="preferences-fields">
          <label className="field">
            <span>Artistas favoritos</span>
            <input
              className="input"
              value={artistas}
              onChange={(evento) => setArtistas(evento.target.value)}
              placeholder="Ej. Banda B, Artista A"
            />
          </label>

          <label className="field">
            <span>Ciudad preferida</span>
            <div className="input-with-action">
              <MapPin size={16} />
              <input
                className="input"
                value={ciudad}
                onChange={(evento) => setCiudad(evento.target.value)}
                placeholder="Ej. Rosario"
              />
            </div>
          </label>
        </div>

        {error ? (
          <p className="preferences-error" role="alert">
            {error}
          </p>
        ) : null}

        <div className="preferences-actions">
          <button type="submit" className="btn btn-primary" disabled={saving || loading}>
            <Save size={16} />
            {saving ? 'Guardando…' : 'Guardar preferencias'}
          </button>
          <button
            type="button"
            className="btn btn-ghost"
            onClick={() => void limpiar()}
            disabled={saving || loading || !tieneDatos}
          >
            <Trash2 size={16} />
            Limpiar
          </button>
        </div>
      </form>
    </section>
  )
}
