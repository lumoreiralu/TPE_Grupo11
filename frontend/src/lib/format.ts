const currencyFormatter = new Intl.NumberFormat('es-UY', {
  style: 'currency',
  currency: 'UYU',
  maximumFractionDigits: 0,
})

const dateFormatter = new Intl.DateTimeFormat('es-UY', {
  weekday: 'short',
  day: '2-digit',
  month: 'short',
  year: 'numeric',
})

const longDateFormatter = new Intl.DateTimeFormat('es-UY', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})

const timeFormatter = new Intl.DateTimeFormat('es-UY', {
  hour: '2-digit',
  minute: '2-digit',
})

export function formatPrice(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '—'
  }
  return currencyFormatter.format(value)
}

export function parseDate(value?: string | null): Date | null {
  if (!value) return null
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

export function formatDate(value?: string | null): string {
  const date = parseDate(value)
  return date ? dateFormatter.format(date) : 'Fecha a confirmar'
}

export function formatLongDate(value?: string | null): string {
  const date = parseDate(value)
  return date ? longDateFormatter.format(date) : 'Fecha a confirmar'
}

export function formatTime(value?: string | null): string {
  const date = parseDate(value)
  return date ? timeFormatter.format(date) : ''
}

export function formatDateTime(value?: string | null): string {
  const date = parseDate(value)
  if (!date) return 'Fecha a confirmar'
  const time = timeFormatter.format(date)
  return time ? `${dateFormatter.format(date)} · ${time} hs` : dateFormatter.format(date)
}

export function formatCountdown(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds))
  const minutes = Math.floor(safe / 60)
  const seconds = safe % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

export function formatShortId(value?: string | null): string {
  if (!value) return '—'
  return value.length > 12 ? `${value.slice(0, 8)}…` : value
}

export function seatLabel(fila?: string | null, asiento?: number | null): string {
  if (fila && asiento !== null && asiento !== undefined) {
    return `Fila ${fila} · Asiento ${asiento}`
  }
  if (asiento !== null && asiento !== undefined) {
    return `Asiento ${asiento}`
  }
  return 'Entrada general'
}
