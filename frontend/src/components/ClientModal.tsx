import { useEffect, useRef, useState, type FormEvent } from 'react'
import { RefreshCw, User, X } from 'lucide-react'
import { generateClienteId } from '../lib/storage'

interface ClientModalProps {
  open: boolean
  clienteId: string
  onClose: () => void
  onSave: (clienteId: string) => void
}

export function ClientModal({ open, clienteId, onClose, onSave }: ClientModalProps) {
  const [value, setValue] = useState(clienteId)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!open) return
    setValue(clienteId)
    const timer = window.setTimeout(() => inputRef.current?.focus(), 50)
    return () => window.clearTimeout(timer)
  }, [open, clienteId])

  useEffect(() => {
    if (!open) return
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [open, onClose])

  if (!open) return null

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSave(value.trim() || generateClienteId())
    onClose()
  }

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <div className="modal-card" role="dialog" aria-modal="true" aria-labelledby="cliente-title">
        <div className="modal-head">
          <div className="modal-icon">
            <User size={20} />
          </div>
          <div>
            <h2 id="cliente-title">Identidad de cliente</h2>
            <p className="muted">
              TicketYa no usa login: este identificador asocia tus compras y entradas.
            </p>
          </div>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Cerrar">
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          <label className="field">
            <span>ID de cliente</span>
            <div className="input-with-action">
              <input
                ref={inputRef}
                className="input"
                value={value}
                onChange={(event) => setValue(event.target.value)}
                placeholder="CLI-123456"
                maxLength={60}
              />
              <button
                type="button"
                className="btn btn-ghost btn-compact"
                onClick={() => setValue(generateClienteId())}
                title="Generar un ID nuevo"
              >
                <RefreshCw size={15} />
                Generar
              </button>
            </div>
          </label>

          <p className="hint">
            Usá el mismo ID en todos tus dispositivos para recuperar tus entradas.
          </p>

          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={onClose}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary">
              Guardar identidad
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
