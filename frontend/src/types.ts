export type TipoEvento = 'RECITAL' | 'TEATRO' | 'DEPORTE' | 'CONFERENCIA'

export type EstadoEvento = 'BORRADOR' | 'PUBLICADO' | 'CANCELADO' | 'FINALIZADO'

export type TipoSector = 'NUMERADO' | 'CAPACIDAD_GENERAL'

export type EstadoOrden =
  | 'RESERVADA_TEMPORAL'
  | 'PROCESANDO_PAGO'
  | 'PAGO_CONFIRMADO'
  | 'COMPLETADA'
  | 'EXPIRADA'
  | 'CANCELADA'

export type EstadoEntrada = 'EMITIDA' | 'USADA' | 'CANCELADA'

export interface PrecioSector {
  idSector: string
  precio: number
}

export interface Evento {
  id: string
  nombre: string
  descripcion?: string
  tipoEvento?: TipoEvento
  organizadorId?: string
  participantes?: string[]
  fechaHora: string
  lugarId: string
  ciudad?: string
  estado?: EstadoEvento
  preciosSectores?: PrecioSector[]
}

export interface FilaSector {
  fila: string
  asientos: number[]
}

export interface Sector {
  sectorId: string
  nombre: string
  tipo: TipoSector
  capacidadMaxima?: number
  filas?: FilaSector[]
}

export interface Lugar {
  id: string
  nombre: string
  direccion?: string
  ciudad?: string
  ubicacion?: {
    latitud: number
    longitud: number
  }
  sectores?: Sector[]
}

export interface ReservaItem {
  sectorId: string
  fila: string
  asiento: number
}

export interface ReservaRequest {
  clienteId: string
  eventoId: string
  items: ReservaItem[]
}

export interface ReservaResponse {
  ordenId: string
  estado: EstadoOrden
  expiraEn: string
  montoTotal: number
  segundosRestantes: number
}

export interface Entrada {
  id: string
  ordenCompraId: string
  clienteId: string
  eventoId: string
  sectorNombre: string
  fila: string | null
  asiento: number | null
  codigoQrHash: string
  estado: EstadoEntrada
  fechaEmision: string
}

export interface ConfirmacionPagoResponse {
  ordenId: string
  estado: EstadoOrden
  transaccionPagoId: string | null
  montoTotal: number
  metodoPago: string | null
  entradas: Entrada[]
  mensaje: string
}

export interface BusquedaEventosParams {
  tipo?: string
  artista?: string
  ciudad?: string
  fechaDesde?: string
  fechaHasta?: string
  organizadorId?: string
}
