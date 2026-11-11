import { Armchair, Info } from 'lucide-react'
import type { Sector } from '../types'
import type { SelectedSeat } from '../lib/storage'
import { formatPrice } from '../lib/format'

interface SeatMapProps {
  sector: Sector
  precio: number
  selected: SelectedSeat[]
  maxEntradas: number
  onToggle: (sector: Sector, fila: string, asiento: number, precio: number) => void
}

export function SeatMap({
  sector,
  precio,
  selected,
  maxEntradas,
  onToggle,
}: SeatMapProps) {
  const selectedKeys = new Set(
    selected.map((seat) => `${seat.sectorId}|${seat.fila}|${seat.asiento}`),
  )
  const atLimit = selected.length >= maxEntradas
  const filas = sector.filas ?? []

  return (
    <section className="sector-card">
      <header className="sector-head">
        <div>
          <span className="sector-kicker">
            <Armchair size={14} />
            {sector.tipo === 'CAPACIDAD_GENERAL' ? 'Capacidad general' : 'Asientos numerados'}
          </span>
          <h3>{sector.nombre}</h3>
        </div>
        <span className="sector-price">{formatPrice(precio)}</span>
      </header>

      {sector.tipo === 'CAPACIDAD_GENERAL' ? (
        <div className="notice">
          <Info size={17} />
          <p>
            Este sector es de capacidad general
            {sector.capacidadMaxima ? ` (${sector.capacidadMaxima} lugares)` : ''}. La reserva de
            entradas generales va a estar disponible en una próxima versión; por ahora elegí un
            sector numerado.
          </p>
        </div>
      ) : filas.length === 0 ? (
        <div className="notice">
          <Info size={17} />
          <p>Este sector todavía no tiene filas y asientos configurados.</p>
        </div>
      ) : (
        <>
          <div className="stage">ESCENARIO</div>
          <div className="seat-map" role="group" aria-label={`Asientos de ${sector.nombre}`}>
            {filas.map((fila) => (
              <div className="seat-row" key={fila.fila}>
                <span className="seat-row-label">F{fila.fila}</span>
                <div className="seat-row-seats">
                  {fila.asientos.map((asiento) => {
                    const key = `${sector.sectorId}|${fila.fila}|${asiento}`
                    const isSelected = selectedKeys.has(key)
                    return (
                      <button
                        key={asiento}
                        type="button"
                        className={`seat ${isSelected ? 'selected' : ''}`}
                        aria-pressed={isSelected}
                        aria-label={`Fila ${fila.fila}, asiento ${asiento}`}
                        title={`Fila ${fila.fila}, asiento ${asiento}`}
                        disabled={!isSelected && atLimit}
                        onClick={() => onToggle(sector, fila.fila, asiento, precio)}
                      >
                        {asiento}
                      </button>
                    )
                  })}
                </div>
              </div>
            ))}
          </div>

          <div className="seat-legend">
            <span>
              <i className="legend-dot available" /> Disponible
            </span>
            <span>
              <i className="legend-dot selected" /> Elegido
            </span>
            {atLimit ? (
              <span className="muted small">Alcanzaste el máximo de {maxEntradas} entradas.</span>
            ) : null}
          </div>
        </>
      )}
    </section>
  )
}
