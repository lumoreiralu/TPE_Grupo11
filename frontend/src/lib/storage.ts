import type { Entrada, EstadoOrden, ReservaItem } from '../types'

const CLIENTE_KEY = 'ticketya.clienteId.v1'
const ORDERS_KEY = 'ticketya.orders.v1'

export interface SelectedSeat extends ReservaItem {
  sectorNombre: string
  precio: number
}

export interface SavedOrder {
  ordenId: string
  eventoId: string
  eventoNombre: string
  eventoFecha: string
  lugarNombre: string
  ciudad: string
  items: SelectedSeat[]
  montoTotal: number
  estado: EstadoOrden
  expiraEn?: string
  createdAt: string
  transaccionPagoId?: string | null
  metodoPago?: string | null
  entradas?: Entrada[]
}

function readJson<T>(key: string, fallback: T): T {
  try {
    const raw = window.localStorage.getItem(key)
    if (!raw) return fallback
    return JSON.parse(raw) as T
  } catch {
    return fallback
  }
}

function writeJson(key: string, value: unknown): void {
  try {
    window.localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // El almacenamiento local puede no estar disponible en modo privado.
  }
}

export function generateClienteId(): string {
  const random = Math.floor(100000 + Math.random() * 900000)
  return `CLI-${random}`
}

export function getClienteId(): string {
  try {
    const stored = window.localStorage.getItem(CLIENTE_KEY)
    if (stored && stored.trim()) return stored.trim()
  } catch {
    // Ignorar errores de almacenamiento.
  }
  return ensureClienteId()
}

export function ensureClienteId(): string {
  const current = (() => {
    try {
      return window.localStorage.getItem(CLIENTE_KEY) ?? ''
    } catch {
      return ''
    }
  })()

  if (current.trim()) return current.trim()

  const generated = generateClienteId()
  setClienteId(generated)
  return generated
}

export function setClienteId(clienteId: string): string {
  const normalized = clienteId.trim() || generateClienteId()
  try {
    window.localStorage.setItem(CLIENTE_KEY, normalized)
  } catch {
    // Ignorar errores de almacenamiento.
  }
  return normalized
}

export function getOrders(): SavedOrder[] {
  const orders = readJson<SavedOrder[]>(ORDERS_KEY, [])
  if (!Array.isArray(orders)) return []
  return orders.sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))
}

export function getOrder(ordenId: string): SavedOrder | null {
  return getOrders().find((order) => order.ordenId === ordenId) ?? null
}

export function saveOrder(order: SavedOrder): void {
  const orders = getOrders()
  const index = orders.findIndex((item) => item.ordenId === order.ordenId)
  if (index >= 0) {
    orders[index] = { ...orders[index], ...order }
  } else {
    orders.unshift(order)
  }
  writeJson(ORDERS_KEY, orders)
}

export function updateOrder(ordenId: string, patch: Partial<SavedOrder>): SavedOrder | null {
  const orders = getOrders()
  const index = orders.findIndex((item) => item.ordenId === ordenId)
  if (index < 0) return null
  const updated = { ...orders[index], ...patch }
  orders[index] = updated
  writeJson(ORDERS_KEY, orders)
  return updated
}

export function getLocalTickets(): Entrada[] {
  return getOrders().flatMap((order) => order.entradas ?? [])
}
