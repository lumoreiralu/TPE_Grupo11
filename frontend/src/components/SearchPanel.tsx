import { Search, SlidersHorizontal, X } from 'lucide-react'
import type { BusquedaEventosParams } from '../types'

interface SearchPanelProps {
  filters: BusquedaEventosParams
  loading?: boolean
  onChange: (filters: BusquedaEventosParams) => void
  onSubmit: () => void
  onReset: () => void
}

export function SearchPanel({
  filters,
  loading = false,
  onChange,
  onSubmit,
  onReset,
}: SearchPanelProps) {
  function update(key: keyof BusquedaEventosParams, value: string) {
    onChange({ ...filters, [key]: value || undefined })
  }

  return (
    <form
      className="search-panel"
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit()
      }}
    >
      <div className="search-panel-head">
        <span className="search-panel-title">
          <SlidersHorizontal size={17} />
          Buscar eventos
        </span>
        <span className="muted small">Filtrá por artista, ciudad, tipo o fecha</span>
      </div>

      <div className="search-grid">
        <label className="field">
          <span>Artista o participante</span>
          <input
            className="input"
            value={filters.artista ?? ''}
            onChange={(event) => update('artista', event.target.value)}
            placeholder="Ej. Banda B"
          />
        </label>

        <label className="field">
          <span>Ciudad</span>
          <input
            className="input"
            value={filters.ciudad ?? ''}
            onChange={(event) => update('ciudad', event.target.value)}
            placeholder="Ej. Montevideo"
          />
        </label>

        <label className="field">
          <span>Tipo de evento</span>
          <select
            className="input select"
            value={filters.tipo ?? ''}
            onChange={(event) => update('tipo', event.target.value)}
          >
            <option value="">Todos</option>
            <option value="RECITAL">Recital</option>
            <option value="TEATRO">Teatro</option>
            <option value="DEPORTE">Deporte</option>
            <option value="CONFERENCIA">Conferencia</option>
          </select>
        </label>

        <label className="field">
          <span>Desde</span>
          <input
            type="date"
            className="input"
            value={filters.fechaDesde ?? ''}
            onChange={(event) => update('fechaDesde', event.target.value)}
          />
        </label>

        <label className="field">
          <span>Hasta</span>
          <input
            type="date"
            className="input"
            value={filters.fechaHasta ?? ''}
            onChange={(event) => update('fechaHasta', event.target.value)}
          />
        </label>

        <div className="search-actions">
          <button type="submit" className="btn btn-primary" disabled={loading}>
            <Search size={16} />
            {loading ? 'Buscando…' : 'Buscar'}
          </button>
          <button type="button" className="btn btn-ghost" onClick={onReset} disabled={loading}>
            <X size={16} />
            Limpiar
          </button>
        </div>
      </div>
    </form>
  )
}
