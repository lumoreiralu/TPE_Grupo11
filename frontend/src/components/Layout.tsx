import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { Ticket, UserRound } from 'lucide-react'
import { useApp } from '../context/AppContext'
import { LoginModal } from './LoginModal'



const navClassName = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'nav-link active' : 'nav-link'

export function Layout() {

  const { clienteId, username, roles, isAuthenticated, isOrganizer, login, logout } = useApp()

  const [modalOpen, setModalOpen] = useState(false)
  
  const roleLabel = roles.includes('ROLE_ADMINISTRADOR')
  ? 'Admin'
  : roles[0]?.replace(/^ROLE_/, '') || 'Usuario'

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="container header-inner">
          <Link to="/" className="brand" aria-label="TicketYa, ir al inicio">
            <span className="brand-mark">
              <Ticket size={21} strokeWidth={2.2} />
            </span>
            <span className="brand-name">
              Ticket<span>Ya</span>
            </span>
          </Link>

          <nav className="main-nav" aria-label="Navegación principal">
            <NavLink to="/" end className={navClassName}>
              Eventos
            </NavLink>
            <NavLink to="/mis-entradas" className={navClassName}>
              Mis entradas
            </NavLink>
            {isOrganizer ? (
              <NavLink to="/eventos/nuevo" className="nav-link">
                Crear evento
              </NavLink>
            ) : null}
          </nav>

    <button
      type="button"
      className="cliente-chip"
      onClick={() => setModalOpen(true)}
      title={isAuthenticated ? 'Ver sesión' : 'Iniciar sesión'}
    >
      <UserRound size={16} />
      <span className="cliente-chip-label">
        {isAuthenticated ? roleLabel : 'Invitado'}
      </span>
      <strong>{isAuthenticated ? (username || clienteId) : 'Iniciar sesión'}</strong>
    </button>
        </div>
      </header>

      <main className="site-main">
        <Outlet />
      </main>

      <footer className="site-footer">
        <div className="container footer-inner">
          <div className="footer-brand">
            <span className="brand-mark small">
              <Ticket size={16} />
            </span>
            <span>
              <strong>TicketYa</strong> · entradas para eventos
            </span>
          </div>
          <p className="footer-note">
            Frontend React consumiendo la API a través del <code>api-gateway</code>.
          </p>
        </div>
      </footer>

      <LoginModal
        open={modalOpen}
        isAuthenticated={isAuthenticated}
        clienteId={username || clienteId}
        onClose={() => setModalOpen(false)}
        onLoginSuccess={(auth) => login(auth)}
        onLogout={logout}
      />
    </div>
  )
}