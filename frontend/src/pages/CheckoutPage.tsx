import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import {
  ArrowLeft,
  CheckCircle2,
  CreditCard,
  Landmark,
  ShieldCheck,
  Ticket,
  Wallet,
} from 'lucide-react'
import { confirmarPago, consultarOrden } from '../api/client'
import type { ConfirmacionPagoResponse } from '../types'
import {
  getOrder,
  saveOrder,
  updateOrder,
  type SavedOrder,
} from '../lib/storage'
import { formatDateTime, formatPrice, formatShortId, seatLabel } from '../lib/format'
import { getErrorMessage } from '../lib/errors'
import { Countdown } from '../components/Countdown'
import { OrderStatusBadge } from '../components/StatusBadge'
import { TicketCard } from '../components/TicketCard'
import { ErrorBlock, LoadingBlock } from '../components/Feedback'

const PAYMENT_METHODS = [
  {
    value: 'TARJETA',
    label: 'Tarjeta de crédito o débito',
    description: 'Pago inmediato simulado',
    Icon: CreditCard,
  },
  {
    value: 'MERCADO_PAGO',
    label: 'Mercado Pago',
    description: 'Billetera virtual simulada',
    Icon: Wallet,
  },
  {
    value: 'TRANSFERENCIA',
    label: 'Transferencia bancaria',
    description: 'Confirmación simulada',
    Icon: Landmark,
  },
] as const

interface CheckoutState {
  order?: SavedOrder
}

export function CheckoutPage() {
  const { ordenId } = useParams<{ ordenId: string }>()
  const location = useLocation()
  const stateOrder = (location.state as CheckoutState | null)?.order

  const [order, setOrder] = useState<SavedOrder | null>(() => {
    if (!ordenId) return null
    return stateOrder ?? getOrder(ordenId)
  })
  const [serverOrder, setServerOrder] = useState<ConfirmacionPagoResponse | null>(null)
  const [success, setSuccess] = useState<ConfirmacionPagoResponse | null>(null)
  const [metodoPago, setMetodoPago] = useState('TARJETA')
  const [checking, setChecking] = useState(true)
  const [confirming, setConfirming] = useState(false)
  const [expired, setExpired] = useState(false)
  const [lookupError, setLookupError] = useState<string | null>(null)
  const [payError, setPayError] = useState<string | null>(null)

  useEffect(() => {
    if (stateOrder) saveOrder(stateOrder)
  }, [stateOrder])

  useEffect(() => {
    if (!ordenId) return
    let active = true
    setChecking(true)
    setLookupError(null)
    setExpired(false)

    consultarOrden(ordenId)
      .then((response) => {
        if (!active) return
        setServerOrder(response)

        const base = getOrder(ordenId)
        if (base) {
          const updated: SavedOrder = {
            ...base,
            estado: response.estado,
            montoTotal: Number(response.montoTotal),
            transaccionPagoId: response.transaccionPagoId,
            metodoPago: response.metodoPago,
            entradas: response.entradas ?? base.entradas,
          }
          saveOrder(updated)
          setOrder(updated)
        }

        if (response.estado === 'COMPLETADA') {
          setSuccess(response)
        }
      })
      .catch((lookupFailure) => {
        if (!active) return
        setLookupError(
          getErrorMessage(lookupFailure, 'No pudimos consultar el estado de la orden.'),
        )
      })
      .finally(() => {
        if (active) setChecking(false)
      })

    return () => {
      active = false
    }
  }, [ordenId])

  const handleExpire = useCallback(() => {
    setExpired(true)
  }, [])

  async function handleConfirm() {
    if (!ordenId) return
    setConfirming(true)
    setPayError(null)

    try {
      const response = await confirmarPago(ordenId, metodoPago)
      setServerOrder(response)
      setSuccess(response)

      const base = getOrder(ordenId)
      if (base) {
        const updated: SavedOrder = {
          ...base,
          estado: response.estado,
          montoTotal: Number(response.montoTotal),
          transaccionPagoId: response.transaccionPagoId,
          metodoPago: response.metodoPago,
          entradas: response.entradas ?? base.entradas,
        }
        saveOrder(updated)
        setOrder(updated)
      }
    } catch (confirmFailure) {
      setPayError(
        getErrorMessage(
          confirmFailure,
          'No pudimos confirmar el pago. Intentá nuevamente.',
        ),
      )

      try {
        const refreshed = await consultarOrden(ordenId)
        setServerOrder(refreshed)
        const base = getOrder(ordenId)
        if (base) {
          const updated = updateOrder(ordenId, {
            estado: refreshed.estado,
            entradas: refreshed.entradas ?? base.entradas,
          })
          if (updated) setOrder(updated)
        }
        if (refreshed.estado === 'COMPLETADA') {
          setSuccess(refreshed)
        }
      } catch {
        // El error original ya se muestra; el refresco es best-effort.
      }
    } finally {
      setConfirming(false)
    }
  }

  if (!ordenId) {
    return (
      <div className="page">
        <div className="container page-pad">
          <ErrorBlock message="No se indicó una orden para pagar." />
        </div>
      </div>
    )
  }

  if (success) {
    const total = Number(success.montoTotal)

    return (
      <div className="page">
        <div className="container page-pad narrow">
          <Link to="/" className="back-link">
            <ArrowLeft size={16} />
            Volver a eventos
          </Link>

          <section className="success-hero">
            <span className="success-icon">
              <CheckCircle2 size={34} />
            </span>
            <span className="section-kicker">Pago confirmado</span>
            <h1>¡Tu compra está completa!</h1>
            <p>{success.mensaje}</p>
          </section>

          <div className="card success-summary">
            <div className="success-summary-head">
              <div>
                <h2>{order?.eventoNombre ?? 'Orden de compra'}</h2>
                <p className="muted">{formatDateTime(order?.eventoFecha)}</p>
              </div>
              <OrderStatusBadge estado={success.estado} />
            </div>

            <div className="summary-detail-grid">
              <div>
                <span>Orden</span>
                <strong>{formatShortId(success.ordenId)}</strong>
              </div>
              <div>
                <span>Total</span>
                <strong>{formatPrice(total)}</strong>
              </div>
              <div>
                <span>Medio de pago</span>
                <strong>{success.metodoPago ?? metodoPago}</strong>
              </div>
              <div>
                <span>Transacción</span>
                <strong>{formatShortId(success.transaccionPagoId)}</strong>
              </div>
            </div>
          </div>

          {success.entradas.length > 0 ? (
            <section className="success-tickets">
              <div className="section-head compact">
                <div>
                  <span className="section-kicker">
                    <Ticket size={15} />
                    Entradas emitidas
                  </span>
                  <h2>Tus entradas</h2>
                </div>
                <p className="muted">
                  {success.entradas.length} entrada
                  {success.entradas.length === 1 ? '' : 's'} con código QR
                </p>
              </div>
              <div className="ticket-list">
                {success.entradas.map((ticket) => (
                  <TicketCard key={ticket.id} ticket={ticket} order={order} />
                ))}
              </div>
            </section>
          ) : (
            <div className="notice banner">
              <Ticket size={17} />
              <p>
                El pago se confirmó. Las entradas se están emitiendo; podés verlas en “Mis
                entradas” en unos instantes.
              </p>
            </div>
          )}

          <div className="success-actions">
            <Link to="/mis-entradas" className="btn btn-primary">
              Ver mis entradas
            </Link>
            <Link to="/" className="btn btn-ghost">
              Seguir buscando eventos
            </Link>
          </div>
        </div>
      </div>
    )
  }

  if (!order && !serverOrder && checking) {
    return (
      <div className="page">
        <div className="container page-pad">
          <LoadingBlock label="Consultando tu orden…" />
        </div>
      </div>
    )
  }

  if (!order && !serverOrder) {
    return (
      <div className="page">
        <div className="container page-pad">
          <Link to="/" className="back-link">
            <ArrowLeft size={16} />
            Volver a eventos
          </Link>
          <ErrorBlock
            message={
              lookupError ??
              'No encontramos esta orden. Puede haber expirado o pertenecer a otro cliente.'
            }
          />
        </div>
      </div>
    )
  }

  const currentStatus = serverOrder?.estado ?? order?.estado
  const total = Number(serverOrder?.montoTotal ?? order?.montoTotal ?? 0)
  const isExpired = expired || currentStatus === 'EXPIRADA'
  const isCancelled = currentStatus === 'CANCELADA'
  const payable =
    !currentStatus ||
    currentStatus === 'RESERVADA_TEMPORAL' ||
    currentStatus === 'PROCESANDO_PAGO' ||
    currentStatus === 'PAGO_CONFIRMADO'
  const canPay = payable && !isExpired && !isCancelled && !confirming

  return (
    <div className="page">
      <div className="container page-pad">
        <Link
          to={order ? `/eventos/${order.eventoId}` : '/'}
          className="back-link"
        >
          <ArrowLeft size={16} />
          {order ? 'Volver al evento' : 'Volver a eventos'}
        </Link>

        <div className="checkout-head">
          <span className="section-kicker">Checkout</span>
          <h1>Confirmá tu compra</h1>
          <p className="hero-lead">
            Revisá tus asientos y elegí un medio de pago. El pago es simulado: no se cobra
            dinero real.
          </p>
        </div>

        <div className="checkout-layout">
          <div className="checkout-main">
            <section className="card order-card">
              <div className="order-card-head">
                <div>
                  <h2>{order?.eventoNombre ?? 'Orden de compra'}</h2>
                  <p className="muted">
                    {order ? formatDateTime(order.eventoFecha) : `Orden ${formatShortId(ordenId)}`}
                  </p>
                </div>
                <OrderStatusBadge estado={currentStatus} />
              </div>

              <div className="summary-detail-grid">
                <div>
                  <span>Orden</span>
                  <strong>{formatShortId(ordenId)}</strong>
                </div>
                <div>
                  <span>Recinto</span>
                  <strong>{order?.lugarNombre || 'A confirmar'}</strong>
                </div>
                <div>
                  <span>Ciudad</span>
                  <strong>{order?.ciudad || 'A confirmar'}</strong>
                </div>
                <div>
                  <span>Entradas</span>
                  <strong>{order?.items.length ?? '—'}</strong>
                </div>
              </div>

              {order?.items.length ? (
                <ul className="order-items">
                  {order.items.map((item) => (
                    <li key={`${item.sectorId}-${item.fila}-${item.asiento}`}>
                      <div>
                        <strong>{item.sectorNombre}</strong>
                        <span>{seatLabel(item.fila, item.asiento)}</span>
                      </div>
                      <span>{formatPrice(item.precio)}</span>
                    </li>
                  ))}
                </ul>
              ) : null}

              <div className="order-total">
                <span>Total a pagar</span>
                <strong>{formatPrice(total)}</strong>
              </div>
            </section>

            <section className="card payment-card">
              <div className="section-head compact">
                <div>
                  <span className="section-kicker">
                    <CreditCard size={15} />
                    Pago
                  </span>
                  <h2>Elegí el medio de pago</h2>
                </div>
              </div>

              <div className="payment-options">
                {PAYMENT_METHODS.map(({ value, label, description, Icon }) => (
                  <label
                    key={value}
                    className={`payment-option ${metodoPago === value ? 'selected' : ''}`}
                  >
                    <input
                      type="radio"
                      name="metodoPago"
                      value={value}
                      checked={metodoPago === value}
                      onChange={() => setMetodoPago(value)}
                    />
                    <span className="payment-option-icon">
                      <Icon size={20} />
                    </span>
                    <span className="payment-option-copy">
                      <strong>{label}</strong>
                      <small>{description}</small>
                    </span>
                    <span className="radio-dot" aria-hidden="true" />
                  </label>
                ))}
              </div>

              <p className="hint">
                <ShieldCheck size={14} />
                La pasarela es un mock académico. Cualquier medio seleccionado se procesa de
                forma simulada.
              </p>
            </section>
          </div>

          <aside className="checkout-aside">
            <div className="card summary-card">
              <div className="summary-row">
                <span>Tiempo para pagar</span>
                <Countdown expiresAt={order?.expiraEn} onExpire={handleExpire} />
              </div>

              {checking ? <p className="hint">Sincronizando estado de la orden…</p> : null}

              {isExpired ? (
                <p className="form-note warning">
                  La reserva expiró y los asientos fueron liberados. Volvé al evento para elegir
                  otros.
                </p>
              ) : null}

              {isCancelled ? (
                <p className="form-note error">
                  La orden fue cancelada. Si el pago fue rechazado, podés intentar una nueva
                  compra desde el evento.
                </p>
              ) : null}

              {lookupError ? <p className="form-note warning">{lookupError}</p> : null}
              {payError ? <p className="form-note error">{payError}</p> : null}

              <button
                type="button"
                className="btn btn-primary btn-block"
                disabled={!canPay}
                onClick={() => void handleConfirm()}
              >
                {confirming ? 'Procesando pago…' : `Pagar ${formatPrice(total)}`}
              </button>

              <p className="hint">
                Al confirmar el pago se emiten tus entradas con un código QR único.
              </p>
            </div>
          </aside>
        </div>
      </div>
    </div>
  )
}
