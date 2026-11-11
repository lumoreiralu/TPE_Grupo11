import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { Ticket, UserRound } from 'lucide-react'
import { useApp } from '../context/AppContext'
import { ClientModal } from './ClientModal'

const navClassName = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'nav-link active' : 'nav-link'

export function Layout() {
  const { clienteId, changeClienteId } = useApp()
  const [modalOpen, setModalOpen] = useState(false)

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
          </nav>

          <button
            type="button"
            className="cliente-chip"
            onClick={() => setModalOpen(true)}
            title="Cambiar identidad de cliente"
          >
            <UserRound size={16} />
            <span className="cliente-chip-label">Cliente</span>
            <strong>{clienteId}</strong>
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

      <ClientModal
        open={modalOpen}
        clienteId={clienteId}
        onClose={() => setModalOpen(false)}
        onSave={changeClienteId}
      />
    </div>
  )
}
