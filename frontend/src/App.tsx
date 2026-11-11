import { useEffect } from 'react'
import {
  BrowserRouter,
  Route,
  Routes,
  useLocation,
} from 'react-router-dom'
import { AppProvider } from './context/AppContext'
import { Layout } from './components/Layout'
import { EventosPage } from './pages/EventosPage'
import { EventoDetallePage } from './pages/EventoDetallePage'
import { CheckoutPage } from './pages/CheckoutPage'
import { EntradasPage } from './pages/EntradasPage'
import { NotFoundPage } from './pages/NotFoundPage'

function ScrollToTop() {
  const { pathname } = useLocation()

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  return null
}

export default function App() {
  return (
    <BrowserRouter>
      <AppProvider>
        <ScrollToTop />
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<EventosPage />} />
            <Route path="eventos/:id" element={<EventoDetallePage />} />
            <Route path="checkout/:ordenId" element={<CheckoutPage />} />
            <Route path="mis-entradas" element={<EntradasPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </AppProvider>
    </BrowserRouter>
  )
}
