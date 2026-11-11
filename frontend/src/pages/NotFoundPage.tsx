import { Link } from 'react-router-dom'
import { Ticket } from 'lucide-react'

export function NotFoundPage() {
  return (
    <div className="page">
      <div className="container page-pad">
        <div className="feedback empty-block">
          <Ticket size={34} />
          <h1>Página no encontrada</h1>
          <p>La ruta que buscás no existe o fue movida.</p>
          <Link to="/" className="btn btn-primary">
            Volver a la cartelera
          </Link>
        </div>
      </div>
    </div>
  )
}
