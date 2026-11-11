import { Drama, Mic2, Music2, Trophy } from 'lucide-react'
import type { TipoEvento } from '../types'

export const EVENT_META = {
  RECITAL: {
    label: 'Recital',
    gradient: 'linear-gradient(135deg, #4c1d95 0%, #a21caf 48%, #ec4899 100%)',
    Icon: Music2,
  },
  TEATRO: {
    label: 'Teatro',
    gradient: 'linear-gradient(135deg, #0f766e 0%, #0891b2 55%, #22d3ee 100%)',
    Icon: Drama,
  },
  DEPORTE: {
    label: 'Deporte',
    gradient: 'linear-gradient(135deg, #9a3412 0%, #ea580c 52%, #f59e0b 100%)',
    Icon: Trophy,
  },
  CONFERENCIA: {
    label: 'Conferencia',
    gradient: 'linear-gradient(135deg, #1e3a8a 0%, #2563eb 52%, #06b6d4 100%)',
    Icon: Mic2,
  },
} as const

export function getEventMeta(tipo?: TipoEvento | string | null) {
  if (tipo && tipo in EVENT_META) {
    return EVENT_META[tipo as TipoEvento]
  }
  return EVENT_META.RECITAL
}
