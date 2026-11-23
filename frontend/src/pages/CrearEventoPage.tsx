import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowLeft, CalendarPlus, CheckCircle2 } from 'lucide-react'
import { crearEvento, getLugares, publicarEvento } from '../api/client'
import { useApp } from '../context/AppContext'
import type { Lugar, ModoFila, PrecioSector, TipoEvento } from '../types'
import { getErrorMessage } from '../lib/errors'
import { EmptyBlock, LoadingBlock } from '../components/Feedback'

const TIPOS_EVENTO: TipoEvento[] = ['RECITAL', 'TEATRO', 'DEPORTE', 'CONFERENCIA']
const MODOS_FILA: ModoFila[] = ['NORMAL', 'ALTA_DEMANDA']

export function CrearEventoPage() {
  const navigate = useNavigate()
  const { clienteId, username, isAuthenticated, isOrganizer } = useApp()
  const organizadorId = username || clienteId

  const [lugares, setLugares] = useState<Lugar[]>([])
  const [loadingLugares, setLoadingLugares] = useState(true)
  const [lugaresError, setLugaresError] = useState<string | null>(null)

  const [nombre, setNombre] = useState('')
  const [descripcion, setDescripcion] = useState('')
  const [tipoEvento, setTipoEvento] = useState<TipoEvento>('RECITAL')
  const [participantesTexto, setParticipantesTexto] = useState('')
  const [fechaHoraLocal, setFechaHoraLocal] = useState('')
  const [lugarId, setLugarId] = useState('')
  const [ciudad, setCiudad] = useState('')
  const [modoFila, setModoFila] = useState<ModoFila>('NORMAL')
  const [umbralConcurrencia, setUmbralConcurrencia] = useState<string>('')
  const [preciosPorSector, setPreciosPorSector] = useState<Record<string, string>>({})

  const [publicarAlCrear, setPublicarAlCrear] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [createdInfo, setCreatedInfo] = useState<{ id: string; estado?: string } | null>(null)

  useEffect(() => {
    if (!isAuthenticated || !isOrganizer) {
      setLoadingLugares(false)
      return
    }

    let active = true
    setLoadingLugares(true)
    setLugaresError(null)

    getLugares()
      .then((data) => {
        if (!active) return
        setLugares(data)
        if (data.length > 0) {
          setLugarId((prev) => prev || data[0].id)
          setCiudad((prev) => prev || data[0].ciudad || '')
        }
      })
      .catch((err) => {
        if (!active) return
        setLugaresError(getErrorMessage(err, 'No se pudieron cargar los lugares.'))
      })
      .finally(() => {
        if (active) setLoadingLugares(false)
      })

    return () => {
      active = false
    }
  }, [isAuthenticated, isOrganizer])

  const lugarSeleccionado = lugares.find((l) => l.id === lugarId) ?? null
  const sectoresDelLugar = lugarSeleccionado?.sectores ?? []

  function handleLugarChange(nextLugarId: string) {
    setLugarId(nextLugarId)
    const found = lugares.find((l) => l.id === nextLugarId)
    if (found?.ciudad) {
      setCiudad(found.ciudad)
    }
    setPreciosPorSector({})
  }

  function handlePrecioChange(sectorId: string, valor: string) {
    setPreciosPorSector((prev) => ({ ...prev, [sectorId]: valor }))
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setFormError(null)
    setCreatedInfo(null)

    if (!nombre.trim()) {
      setFormError('Ingresá el nombre del evento.')
      return
    }
    if (!fechaHoraLocal) {
      setFormError('Seleccioná la fecha y hora del evento.')
      return
    }
    if (!lugarId) {
      setFormError('Seleccioná un recinto.')
      return
    }

    const preciosSectores: PrecioSector[] = sectoresDelLugar
      .map((s) => ({
        idSector: s.sectorId,
        precio: Number(preciosPorSector[s.sectorId] ?? 0),
      }))
      .filter((p) => Number.isFinite(p.precio) && p.precio > 0)

    if (publicarAlCrear && sectoresDelLugar.length > 0 && preciosSectores.length !== sectoresDelLugar.length) {
      setFormError('Para publicar el evento, ingresá un precio mayor a 0 en todos los sectores.')
      return
    }

    const participantes = participantesTexto
      .split(',')
      .map((p) => p.trim())
      .filter(Boolean)

    const fechaHoraIso =
      fechaHoraLocal.length === 16 ? `${fechaHoraLocal}:00` : fechaHoraLocal

    const umbralNum = Number(umbralConcurrencia)

    setSubmitting(true)
    try {
      const creado = await crearEvento({
        nombre: nombre.trim(),
        descripcion: descripcion.trim() || undefined,
        tipoEvento,
        organizadorId,
        participantes: participantes.length > 0 ? participantes : undefined,
        fechaHora: fechaHoraIso,
        lugarId,
        ciudad: ciudad.trim() || lugarSeleccionado?.ciudad || undefined,
        modoFila,
        ...(Number.isFinite(umbralNum) && umbralNum > 0
          ? { umbralConcurrencia: umbralNum }
          : {}),
        preciosSectores,
      })

      if (publicarAlCrear && preciosSectores.length > 0) {
        const publicado = await publicarEvento(creado.id, preciosSectores)
        setCreatedInfo({ id: publicado.id, estado: publicado.estado })
      } else {
        setCreatedInfo({ id: creado.id, estado: creado.estado })
      }
    } catch (err) {
      setFormError(getErrorMessage(err, 'No se pudo crear el evento.'))
    } finally {
      setSubmitting(false)
    }
  }

  if (!isAuthenticated || !isOrganizer) {
    return (
      <div className="page">
        <div className="container page-pad">
          <EmptyBlock
            title="Acceso restringido a organizadores"
            description="Iniciá sesión con una cuenta con rol de organizador para crear eventos."
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

  if (loadingLugares) {
    return (
      <div className="page">
        <div className="container page-pad">
          <LoadingBlock label="Cargando recintos disponibles…" />
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
              <CalendarPlus size={15} />
              Panel organizador
            </span>
            <h1>Crear nuevo evento</h1>
            <p className="muted">
              Organizador: <strong>{organizadorId}</strong>
            </p>
          </div>
        </div>

        {lugaresError ? <p className="form-note error">{lugaresError}</p> : null}

        {createdInfo ? (
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <CheckCircle2 size={20} />
              <strong>
                Evento {createdInfo.estado === 'PUBLICADO' ? 'creado y publicado' : 'creado en borrador'}
              </strong>
            </div>
            <p className="muted">ID del evento: {createdInfo.id}</p>
            <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem', flexWrap: 'wrap' }}>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => navigate(`/eventos/${createdInfo.id}`)}
              >
                Ver evento
              </button>
              <Link to="/" className="btn btn-ghost">
                Ir a cartelera
              </Link>
            </div>
          </div>
        ) : null}

        <form className="card" onSubmit={(e) => void handleSubmit(e)}>
          <div style={{ display: 'grid', gap: '1rem' }}>
            <label>
              <span>Nombre del evento *</span>
              <input
                className="input"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                placeholder="Ej. Recital TicketYa Tour"
                required
              />
            </label>

            <label>
              <span>Descripción</span>
              <textarea
                className="input"
                rows={3}
                value={descripcion}
                onChange={(e) => setDescripcion(e.target.value)}
                placeholder="Contá de qué trata el evento..."
              />
            </label>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
              <label>
                <span>Tipo de evento *</span>
                <select
                  className="input"
                  value={tipoEvento}
                  onChange={(e) => setTipoEvento(e.target.value as TipoEvento)}
                >
                  {TIPOS_EVENTO.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                <span>Fecha y hora *</span>
                <input
                  type="datetime-local"
                  className="input"
                  value={fechaHoraLocal}
                  onChange={(e) => setFechaHoraLocal(e.target.value)}
                  required
                />
              </label>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
              <label>
                <span>Recinto / Lugar *</span>
                <select
                  className="input"
                  value={lugarId}
                  onChange={(e) => handleLugarChange(e.target.value)}
                  required
                >
                  {lugares.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.nombre} {l.ciudad ? `(${l.ciudad})` : ''}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                <span>Ciudad</span>
                <input
                  className="input"
                  value={ciudad}
                  onChange={(e) => setCiudad(e.target.value)}
                  placeholder="Ej. Tandil o Montevideo"
                />
              </label>
            </div>

            <label>
              <span>Participantes / Artistas (separados por coma)</span>
              <input
                className="input"
                value={participantesTexto}
                onChange={(e) => setParticipantesTexto(e.target.value)}
                placeholder="Ej. Artista A, Banda B"
              />
            </label>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
              <label>
                <span>Modo de fila</span>
                <select
                  className="input"
                  value={modoFila}
                  onChange={(e) => setModoFila(e.target.value as ModoFila)}
                >
                  {MODOS_FILA.map((m) => (
                    <option key={m} value={m}>
                      {m}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                <span>Umbral concurrencia (opcional)</span>
                <input
                  type="number"
                  min={1}
                  className="input"
                  value={umbralConcurrencia}
                  onChange={(e) => setUmbralConcurrencia(e.target.value)}
                  placeholder="Ej. 100"
                />
              </label>
            </div>

            {sectoresDelLugar.length > 0 ? (
              <div>
                <strong>Precios por sector</strong>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.75rem', marginTop: '0.5rem' }}>
                  {sectoresDelLugar.map((sector) => (
                    <label key={sector.sectorId}>
                      <span>
                        {sector.nombre} ({sector.tipo})
                      </span>
                      <input
                        type="number"
                        min={1}
                        step="0.01"
                        className="input"
                        value={preciosPorSector[sector.sectorId] ?? ''}
                        onChange={(e) => handlePrecioChange(sector.sectorId, e.target.value)}
                        placeholder="Precio $"
                      />
                    </label>
                  ))}
                </div>
              </div>
            ) : lugarId ? (
              <p className="muted small">
                Este recinto no tiene sectores cargados; podés crearlo en borrador y cargar sectores/precios luego.
              </p>
            ) : null}

            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <input
                type="checkbox"
                checked={publicarAlCrear}
                onChange={(e) => setPublicarAlCrear(e.target.checked)}
              />
              <span>Publicar evento automáticamente al crearlo</span>
            </label>

            {formError ? <p className="form-note error">{formError}</p> : null}

            <button
              type="submit"
              className="btn btn-primary"
              disabled={submitting || lugares.length === 0}
            >
              {submitting ? 'Guardando…' : publicarAlCrear ? 'Crear y publicar evento' : 'Guardar borrador'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}