import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Eye, EyeOff, Lock, LogOut, User, X } from 'lucide-react'
import { iniciarSesion } from '../api/client'
import type { AuthResponse } from '../types'

interface LoginModalProps {
  open: boolean
  isAuthenticated: boolean
  clienteId: string
  initialUsername?: string
  onClose: () => void
  onLoginSuccess: (auth: AuthResponse) => void
  onLogout: () => void
}

export function LoginModal({
  open,
  isAuthenticated,
  clienteId,
  initialUsername = '',
  onClose,
  onLoginSuccess,
  onLogout,
}: LoginModalProps) {
  const [username, setUsername] = useState(initialUsername)
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!open) return
    setUsername(initialUsername)
    setPassword('')
    setError(null)
    setShowPassword(false)
    const timer = window.setTimeout(() => inputRef.current?.focus(), 50)
    return () => window.clearTimeout(timer)
  }, [open, initialUsername])

  useEffect(() => {
    if (!open) return
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !loading) onClose()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [open, onClose, loading])

  if (!open) return null

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const cleanUser = username.trim()
    if (!cleanUser || !password) {
      setError('Completá usuario y contraseña.')
      return
    }

    setLoading(true)
    setError(null)

    try {
      const data = await iniciarSesion({ username: cleanUser, password })
      onLoginSuccess(data)
      setPassword('')
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo iniciar sesión.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !loading) onClose()
      }}
    >
      <div
        className="modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="login-title"
      >
        <div className="modal-head">
          <div className="modal-icon">
            <Lock size={20} />
          </div>
          <div>
            <h2 id="login-title">
              {isAuthenticated ? 'Sesión activa' : 'Iniciar sesión'}
            </h2>
            <p className="muted">
              {isAuthenticated
                ? `Conectado como ${clienteId}`
                : 'Ingresá tus credenciales para autenticarte con la API.'}
            </p>
          </div>
          <button
            type="button"
            className="icon-button"
            onClick={onClose}
            aria-label="Cerrar"
            disabled={loading}
          >
            <X size={18} />
          </button>
        </div>

        {isAuthenticated ? (
          <div className="modal-body">
            <p className="hint">
              Tu sesión está activa con token JWT. Si cerrás sesión, se eliminará el token guardado en este navegador.
            </p>
            <div className="modal-actions">
              <button type="button" className="btn btn-ghost" onClick={onClose}>
                Cerrar
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => {
                  onLogout()
                  onClose()
                }}
              >
                <LogOut size={15} />
                Cerrar sesión
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="modal-body" noValidate>
            <label className="field">
              <span>Usuario</span>
              <input
                ref={inputRef}
                type="text"
                className="input"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                placeholder="tu_usuario"
                autoComplete="username"
                disabled={loading}
              />
            </label>

            <label className="field">
              <span>Contraseña</span>
              <div className="input-with-action">
                <input
                  type={showPassword ? 'text' : 'password'}
                  className="input"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="••••••••"
                  autoComplete="current-password"
                  disabled={loading}
                />
                <button
                  type="button"
                  className="btn btn-ghost btn-compact"
                  onClick={() => setShowPassword((prev) => !prev)}
                  disabled={loading}
                  aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                >
                  {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              </div>
            </label>

            {error ? (
              <p className="form-note error" role="alert">
                {error}
              </p>
            ) : (
              <p className="hint">
                <User size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} />
                Las credenciales se enviarán a través del gateway de la aplicación.
              </p>
            )}

            <div className="modal-actions">
              <button
                type="button"
                className="btn btn-ghost"
                onClick={onClose}
                disabled={loading}
              >
                Cancelar
              </button>
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Ingresando…' : 'Iniciar sesión'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}