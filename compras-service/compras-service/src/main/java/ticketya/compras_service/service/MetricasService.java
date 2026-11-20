package ticketya.compras_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ticketya.compras_service.dto.RecaudacionResponse;
import ticketya.compras_service.exception.NegocioException;
import ticketya.compras_service.model.EstadoOrden;
import ticketya.compras_service.repository.OrdenCompraRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Métricas de recaudación (R12.1).
 *
 * <p>La recaudación se deriva de las órdenes de compra que ya viven en este
 * servicio; no se agrega una colección nueva ni se duplica el dato de dinero.
 * Se suman las órdenes con pago aprobado y se descuentan las compensadas o
 * reembolsadas para reflejar el dinero neto que quedó en caja.
 */
@Service
public class MetricasService {

    private static final Logger log = LoggerFactory.getLogger(MetricasService.class);

    /** Estados en los que la pasarela ya cobró el dinero (una compensada lo cobró antes de reembolsarse). */
    private static final List<EstadoOrden> ESTADOS_CON_PAGO = List.of(
            EstadoOrden.PAGO_CONFIRMADO,
            EstadoOrden.COMPLETADA,
            EstadoOrden.COMPENSADA);

    /** Estados que representan una venta confirmada y vigente. */
    private static final List<EstadoOrden> ESTADOS_CONFIRMADOS = List.of(
            EstadoOrden.PAGO_CONFIRMADO,
            EstadoOrden.COMPLETADA);

    /** Estados con dinero devuelto al cliente. */
    private static final List<EstadoOrden> ESTADOS_REEMBOLSADOS = List.of(
            EstadoOrden.COMPENSADA);

    private final OrdenCompraRepository ordenRepo;

    public MetricasService(OrdenCompraRepository ordenRepo) {
        this.ordenRepo = ordenRepo;
    }

    @Transactional(readOnly = true)
    public RecaudacionResponse obtenerRecaudacion(String eventoId) {
        if (eventoId == null || eventoId.isBlank()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "El identificador del evento es obligatorio");
        }

        BigDecimal bruta = montoNoNulo(ordenRepo.sumarMontoPorEventoYEstados(eventoId, ESTADOS_CON_PAGO));
        BigDecimal reembolsada = montoNoNulo(ordenRepo.sumarMontoPorEventoYEstados(eventoId, ESTADOS_REEMBOLSADOS));
        long confirmadas = ordenRepo.contarPorEventoYEstados(eventoId, ESTADOS_CONFIRMADOS);
        long compensadas = ordenRepo.contarPorEventoYEstados(eventoId, ESTADOS_REEMBOLSADOS);

        BigDecimal total = bruta.subtract(reembolsada);
        log.debug("Recaudación evento {}: bruta={} reembolsada={} total={} confirmadas={} compensadas={}",
                eventoId, bruta, reembolsada, total, confirmadas, compensadas);

        return new RecaudacionResponse(
                eventoId,
                total,
                bruta,
                reembolsada,
                confirmadas,
                compensadas);
    }

    private BigDecimal montoNoNulo(BigDecimal monto) {
        return monto != null ? monto : BigDecimal.ZERO;
    }
}
