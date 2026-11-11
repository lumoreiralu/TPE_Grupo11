import { useEffect, useRef, useState } from 'react'
import { Clock } from 'lucide-react'
import { formatCountdown } from '../lib/format'

function computeRemaining(expiresAt?: string | null): number | null {
  if (!expiresAt) return null
  const target = new Date(expiresAt).getTime()
  if (Number.isNaN(target)) return null
  return Math.max(0, Math.ceil((target - Date.now()) / 1000))
}

interface CountdownProps {
  expiresAt?: string | null
  onExpire?: () => void
  compact?: boolean
}

export function Countdown({ expiresAt, onExpire, compact = false }: CountdownProps) {
  const [remaining, setRemaining] = useState<number | null>(() => computeRemaining(expiresAt))
  const expiredRef = useRef(false)

  useEffect(() => {
    expiredRef.current = false
    setRemaining(computeRemaining(expiresAt))

    if (!expiresAt) return
    const timer = window.setInterval(() => {
      setRemaining(computeRemaining(expiresAt))
    }, 1000)
    return () => window.clearInterval(timer)
  }, [expiresAt])

  useEffect(() => {
    if (remaining === 0 && expiresAt && !expiredRef.current) {
      expiredRef.current = true
      onExpire?.()
    }
  }, [remaining, expiresAt, onExpire])

  if (remaining === null) return null

  const urgent = remaining > 0 && remaining <= 60

  return (
    <span
      className={`countdown ${urgent ? 'urgent' : ''} ${compact ? 'compact' : ''}`}
      title="La reserva se libera automáticamente al agotarse el tiempo"
    >
      <Clock size={compact ? 14 : 16} />
      {remaining === 0 ? 'Reserva expirada' : formatCountdown(remaining)}
    </span>
  )
}
