import type {
  BusquedaEventosParams,
  ConfirmacionPagoResponse,
  Entrada,
  Evento,
  Lugar,
  ReservaRequest,
  ReservaResponse,
} from '../types'

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

export class ApiError extends Error {
  readonly status: number
  readonly details: unknown

  constructor(status: number, message: string, details?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (!headers.has('Accept')) headers.set('Accept', 'application/json')
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, { ...init, headers })
  } catch {
    throw new ApiError(
      0,
      'No se pudo conectar con el servidor. Verificá que el api-gateway esté corriendo en http://localhost:8080.',
    )
  }

  if (!response.ok) {
    const raw = await response.text()
    let details: unknown = raw
    if (raw) {
      try {
        details = JSON.parse(raw)
      } catch {
        // La respuesta de error no era JSON.
      }
    }

    const body =
      details && typeof details === 'object' ? (details as Record<string, unknown>) : null
    const message =
      (typeof body?.mensaje === 'string' && body.mensaje) ||
      (typeof body?.message === 'string' && body.message) ||
      (typeof body?.error === 'string' && body.error) ||
      `La operación falló con el código ${response.status}.`

    throw new ApiError(response.status, message, details)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

function buildQuery(params: BusquedaEventosParams): string {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      query.set(key, String(value).trim())
    }
  }
  const serialized = query.toString()
  return serialized ? `?${serialized}` : ''
}

export function buscarEventos(params: BusquedaEventosParams = {}): Promise<Evento[]> {
  return request<Evento[]>(`/api/eventos/buscar${buildQuery(params)}`)
}

export function getEvento(id: string): Promise<Evento> {
  return request<Evento>(`/api/eventos/${encodeURIComponent(id)}`)
}

export function getLugar(id: string): Promise<Lugar> {
  return request<Lugar>(`/api/lugares/${encodeURIComponent(id)}`)
}

export function reservar(
  body: ReservaRequest,
  idempotencyKey: string,
): Promise<ReservaResponse> {
  return request<ReservaResponse>('/api/compras/reservar', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey },
    body: JSON.stringify(body),
  })
}

export function consultarOrden(ordenId: string): Promise<ConfirmacionPagoResponse> {
  return request<ConfirmacionPagoResponse>(
    `/api/compras/${encodeURIComponent(ordenId)}`,
  )
}

export function confirmarPago(
  ordenId: string,
  metodoPago: string,
): Promise<ConfirmacionPagoResponse> {
  return request<ConfirmacionPagoResponse>(
    `/api/compras/${encodeURIComponent(ordenId)}/confirmar-pago`,
    {
      method: 'POST',
      body: JSON.stringify({ metodoPago }),
    },
  )
}

export function getEntradasPorOrden(ordenId: string): Promise<Entrada[]> {
  return request<Entrada[]>(`/api/entradas/orden/${encodeURIComponent(ordenId)}`)
}

export function getEntradasPorCliente(clienteId: string): Promise<Entrada[]> {
  return request<Entrada[]>(`/api/entradas/cliente/${encodeURIComponent(clienteId)}`)
}
