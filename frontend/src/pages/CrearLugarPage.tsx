import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, Building2, CheckCircle2, Plus, Trash2 } from 'lucide-react'
import { crearLugar } from '../api/client'
import { useApp } from '../context/AppContext'
import { getErrorMessage } from '../lib/errors'
import { EmptyBlock } from '../components/Feedback'
import type { TipoSector } from '../types'

interface FilaForm {
  fila: string
  asientos: string
}

interface SectorForm {
  nombre: string
  tipo: TipoSector
  capacidadMaxima: string
  filas: FilaForm[]
}

function sectorInicial(): SectorForm {
  return {
    nombre: '',
    tipo: 'NUMERADO',
    capacidadMaxima: '',
    filas: [{ fila: 'A', asientos: '1,2,3,4,5' }],
  }
}

export function CrearLugarPage() {
  const { isAuthenticated, isAdmin } = useApp()
  const [nombre, setNombre] = useState('')
  const [direccion, setDireccion] = useState('')
  const [ciudad, setCiudad] = useState('')
  const [sectores, setSectores] = useState<SectorForm[]>([sectorInicial()])
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [createdLugar, setCreatedLugar] = useState<{ id: string; nombre: string } | null>(null)

  function actualizarSector(index: number, cambios: Partial<SectorForm>) {
    setSectores((prev) => prev.map((sector, i) => (i === index ? { ...sector, ...cambios } : sector)))
  }

  function actualizarFila(sectorIndex: number, filaIndex: number, cambios: Partial<FilaForm>) {
    setSectores((prev) =>
      prev.map((sector, i) =>
        i === sectorIndex
          ? {
              ...sector,
              filas: sector.filas.map((fila, j) => (j === filaIndex ? { ...fila, ...cambios } : fila)),
            }
          : sector,
      ),
    )
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setFormError(null)
    setCreatedLugar(null)

    if (!nombre.trim()) {
      setFormError('Ingresá el nombre del lugar.')
      return
    }
    if (sectores.length === 0) {
      setFormError('Agregá al menos un sector.')
      return
    }

    for (const sector of sectores) {
      if (!sector.nombre.trim()) {
        setFormError('Todos los sectores deben tener un nombre.')
        return
      }
      if (sector.tipo === 'CAPACIDAD_GENERAL') {
        const capacidad = Number(sector.capacidadMaxima)
        if (!Number.isInteger(capacidad) || capacidad <= 0) {
          setFormError(`El sector "${sector.nombre}" necesita una capacidad máxima mayor a cero.`)
          return
        }
      } else {
        if (sector.filas.length === 0) {
          setFormError(`El sector numerado "${sector.nombre}" necesita al menos una fila.`)
          return
        }
        for (const fila of sector.filas) {
          const asientos = fila.asientos
            .split(',')
            .map((valor) => Number(valor.trim()))
            .filter((valor) => Number.isInteger(valor) && valor > 0)
          if (!fila.fila.trim() || asientos.length === 0) {
            setFormError(`Completá el nombre y los asientos de todas las filas de "${sector.nombre}".`)
            return
          }
        }
      }
    }

    setSubmitting(true)
    try {
      const lugar = await crearLugar({
        nombre: nombre.trim(),
        direccion: direccion.trim() || undefined,
        ciudad: ciudad.trim() || undefined,
        sectores: sectores.map((sector, index) => ({
          sectorId: `${sector.nombre.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'sector'}-${index + 1}`,
          nombre: sector.nombre.trim(),
          tipo: sector.tipo,
          ...(sector.tipo === 'CAPACIDAD_GENERAL'
            ? { capacidadMaxima: Number(sector.capacidadMaxima) }
            : {
                filas: sector.filas.map((fila) => ({
                  fila: fila.fila.trim(),
                  asientos: fila.asientos
                    .split(',')
                    .map((valor) => Number(valor.trim()))
                    .filter((valor) => Number.isInteger(valor) && valor > 0),
                })),
              }),
        })),
      })
      setCreatedLugar({ id: lugar.id, nombre: lugar.nombre })
    } catch (err) {
      setFormError(getErrorMessage(err, 'No se pudo crear el lugar.'))
    } finally {
      setSubmitting(false)
    }
  }

  if (!isAuthenticated || !isAdmin) {
    return (
      <div className="page">
        <div className="container page-pad">
          <EmptyBlock
            title="Acceso restringido"
            description="Solo un administrador puede crear lugares y sectores."
            action={
              <Link to="/" className="btn btn-primary">
                Volver a eventos
              </Link>
            }
          />
        </div>
      </div>
    )
  }

  return (
    <div className="page">
      <div className="container page-pad narrow">
        <Link to="/" className="back-link">
          <ArrowLeft size={16} />
          Volver a eventos
        </Link>

        <div className="section-head">
          <div>
            <span className="section-kicker">
              <Building2 size={15} />
              Panel administrador
            </span>
            <h1>Crear lugar para eventos</h1>
          </div>
        </div>

        {createdLugar ? (
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <CheckCircle2 size={20} />
              <strong>Lugar creado correctamente</strong>
            </div>
            <p className="muted">{createdLugar.nombre} · ID: {createdLugar.id}</p>
            <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem', flexWrap: 'wrap' }}>
              <Link to="/eventos/nuevo" className="btn btn-primary">
                Crear evento en este lugar
              </Link>
              <button type="button" className="btn btn-ghost" onClick={() => setCreatedLugar(null)}>
                Crear otro lugar
              </button>
            </div>
          </div>
        ) : null}

        <form className="card" onSubmit={(event) => void handleSubmit(event)}>
          <div style={{ display: 'grid', gap: '1rem' }}>
            <label>
              <span>Nombre del lugar *</span>
              <input className="input" value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Ej. Teatro Central" required />
            </label>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
              <label>
                <span>Dirección</span>
                <input className="input" value={direccion} onChange={(e) => setDireccion(e.target.value)} placeholder="Ej. Av. Principal 123" />
              </label>
              <label>
                <span>Ciudad</span>
                <input className="input" value={ciudad} onChange={(e) => setCiudad(e.target.value)} placeholder="Ej. Tandil" />
              </label>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem', marginTop: '0.5rem' }}>
              <strong>Sectores del lugar</strong>
              <button type="button" className="btn btn-ghost" onClick={() => setSectores((prev) => [...prev, sectorInicial()])}>
                <Plus size={16} />
                Agregar sector
              </button>
            </div>

            {sectores.map((sector, sectorIndex) => (
              <div className="card" key={sectorIndex} style={{ borderRadius: '12px', boxShadow: 'none' }}>
                <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '1rem' }}>
                  <h3 style={{ margin: 0 }}>Sector {sectorIndex + 1}</h3>
                  {sectores.length > 1 ? (
                    <button
                      type="button"
                      className="icon-button"
                      title="Eliminar sector"
                      onClick={() => setSectores((prev) => prev.filter((_, i) => i !== sectorIndex))}
                    >
                      <Trash2 size={16} />
                    </button>
                  ) : null}
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', marginTop: '1rem' }}>
                  <label>
                    <span>Nombre del sector *</span>
                    <input className="input" value={sector.nombre} onChange={(e) => actualizarSector(sectorIndex, { nombre: e.target.value })} placeholder="Ej. Platea Norte" />
                  </label>
                  <label>
                    <span>Tipo de sector *</span>
                    <select className="input" value={sector.tipo} onChange={(e) => actualizarSector(sectorIndex, { tipo: e.target.value as TipoSector })}>
                      <option value="NUMERADO">Numerado</option>
                      <option value="CAPACIDAD_GENERAL">Sin numerar / Capacidad general</option>
                    </select>
                  </label>
                </div>

                {sector.tipo === 'CAPACIDAD_GENERAL' ? (
                  <label style={{ display: 'block', marginTop: '1rem', maxWidth: '260px' }}>
                    <span>Capacidad máxima *</span>
                    <input type="number" min={1} className="input" value={sector.capacidadMaxima} onChange={(e) => actualizarSector(sectorIndex, { capacidadMaxima: e.target.value })} placeholder="Ej. 500" />
                  </label>
                ) : (
                  <div style={{ marginTop: '1rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem', marginBottom: '0.5rem' }}>
                      <strong>Filas y asientos</strong>
                      <button
                        type="button"
                        className="btn btn-ghost btn-compact"
                        onClick={() => actualizarSector(sectorIndex, { filas: [...sector.filas, { fila: '', asientos: '' }] })}
                      >
                        <Plus size={14} />
                        Agregar fila
                      </button>
                    </div>
                    {sector.filas.map((fila, filaIndex) => (
                      <div key={filaIndex} style={{ display: 'grid', gridTemplateColumns: 'minmax(100px, 0.35fr) minmax(220px, 1fr) auto', gap: '0.75rem', alignItems: 'end', marginBottom: '0.75rem' }}>
                        <label>
                          <span>Fila</span>
                          <input className="input" value={fila.fila} onChange={(e) => actualizarFila(sectorIndex, filaIndex, { fila: e.target.value })} placeholder="A" />
                        </label>
                        <label>
                          <span>Asientos (separados por coma)</span>
                          <input className="input" value={fila.asientos} onChange={(e) => actualizarFila(sectorIndex, filaIndex, { asientos: e.target.value })} placeholder="1,2,3,4,5" />
                        </label>
                        {sector.filas.length > 1 ? (
                          <button
                            type="button"
                            className="icon-button"
                            title="Eliminar fila"
                            onClick={() => actualizarSector(sectorIndex, { filas: sector.filas.filter((_, i) => i !== filaIndex) })}
                          >
                            <Trash2 size={16} />
                          </button>
                        ) : null}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}

            {formError ? <p className="form-note error">{formError}</p> : null}

            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Guardando…' : 'Crear lugar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
