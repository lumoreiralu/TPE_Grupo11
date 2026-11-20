package ticketya.compras_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ticketya.compras_service.client.EmisionRequest;
import ticketya.compras_service.client.EmisionResponse;
import ticketya.compras_service.client.EntradasClient;
import ticketya.compras_service.client.PagoRequest;
import ticketya.compras_service.client.PagoResponse;
import ticketya.compras_service.client.PasarelaClient;
import ticketya.compras_service.client.ReembolsoRequest;
import ticketya.compras_service.client.ReembolsoResponse;
import ticketya.compras_service.dto.ConfirmacionPagoResponse;
import ticketya.compras_service.dto.ConfirmarPagoRequest;
import ticketya.compras_service.exception.NegocioException;
import ticketya.compras_service.model.EstadoOrden;
import ticketya.compras_service.model.OrdenCompra;
import ticketya.compras_service.util.Futures;

import java.util.List;
import java.util.UUID;

@Service
public class CompraSagaService {

    private static final Logger log = LoggerFactory.getLogger(CompraSagaService.class);

    private final CompraTransaccionService transaccionService;
    private final PasarelaClient pasarelaClient;
    private final EntradasClient entradasClient;

    public CompraSagaService(CompraTransaccionService transaccionService,
                             PasarelaClient pasarelaClient,
                             EntradasClient entradasClient) {
        this.transaccionService = transaccionService;
        this.pasarelaClient = pasarelaClient;
        this.entradasClient = entradasClient;
    }

    public ConfirmacionPagoResponse confirmarPago(UUID ordenId, ConfirmarPagoRequest request) {
        OrdenCompra orden = transaccionService.iniciarProcesamientoPago(ordenId);

        if (orden.getEstado() == EstadoOrden.EXPIRADA) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "La reserva temporal ha expirado y los asientos han sido liberados");
        }

        if (orden.getEstado() == EstadoOrden.COMPLETADA) {
            List<EmisionResponse.EntradaDTO> entradas =
                    Futures.join(entradasClient.obtenerEntradasPorOrden(orden.getId().toString()));
            return respuesta(orden, entradas, "La orden ya fue completada previamente");
        }

        String metodoPago = request != null && request.metodoPago() != null
                && !request.metodoPago().isBlank()
                ? request.metodoPago()
                : "TARJETA";
        String transaccionAprobada = null;

        try {
            if (orden.getEstado() != EstadoOrden.PAGO_CONFIRMADO) {
                PagoRequest pagoRequest = new PagoRequest(
                        orden.getMontoTotal(), orden.getId().toString(), metodoPago);
                PagoResponse pagoResponse = Futures.join(pasarelaClient.procesarPago(pagoRequest));

                if (pagoResponse == null || !"APROBADO".equalsIgnoreCase(pagoResponse.estado())) {
                    String transaccionId = pagoResponse != null ? pagoResponse.transaccionId() : null;
                    String motivo = pagoResponse != null && pagoResponse.mensaje() != null
                            ? pagoResponse.mensaje()
                            : "Pago rechazado por la pasarela";
                    transaccionService.registrarPagoRechazado(ordenId, transaccionId, metodoPago);
                    throw new NegocioException(HttpStatus.PAYMENT_REQUIRED, "El pago fue rechazado: " + motivo);
                }

                transaccionAprobada = pagoResponse.transaccionId();
                transaccionService.registrarPagoAprobado(ordenId, transaccionAprobada, metodoPago);
            }

            orden = transaccionService.obtenerOrden(ordenId);
            List<EmisionRequest.AsientoEntrada> asientos = orden.getDetalles().stream()
                    .map(detalle -> new EmisionRequest.AsientoEntrada(
                            detalle.getSectorId(), detalle.getFila(), detalle.getAsiento()))
                    .toList();
            EmisionRequest emisionRequest = new EmisionRequest(
                    orden.getId().toString(), orden.getClienteId(), orden.getEventoId(), asientos);
            EmisionResponse emisionResponse = Futures.join(entradasClient.emitirEntradas(emisionRequest));
            List<EmisionResponse.EntradaDTO> entradas = emisionResponse != null && emisionResponse.entradas() != null
                    ? emisionResponse.entradas()
                    : List.of();

            transaccionService.registrarOrdenCompletada(ordenId);
            orden = transaccionService.obtenerOrden(ordenId);
            return respuesta(orden, entradas, "Compra completada exitosamente");
        } catch (RuntimeException error) {
            compensarTrasFallo(ordenId, metodoPago, transaccionAprobada, error);
            throw error;
        }
    }

    public ConfirmacionPagoResponse compensarManualmente(UUID ordenId) {
        OrdenCompra orden = transaccionService.obtenerOrden(ordenId);
        if (orden.getEstado() == EstadoOrden.COMPENSADA) {
            return respuesta(orden, List.of(), "La orden ya estaba compensada");
        }
        if (orden.getEstado() != EstadoOrden.PAGO_CONFIRMADO
                && orden.getEstado() != EstadoOrden.COMPLETADA) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "Solo se pueden compensar órdenes con pago confirmado");
        }

        ejecutarCompensacion(orden, "Compensación manual solicitada por un administrador");
        orden = transaccionService.obtenerOrden(ordenId);
        return respuesta(orden, List.of(), "Orden compensada exitosamente");
    }

    private void compensarTrasFallo(UUID ordenId, String metodoPago,
                                    String transaccionAprobada, RuntimeException error) {
        OrdenCompra orden = transaccionService.obtenerOrden(ordenId);
        if (orden.getEstado() == EstadoOrden.PROCESANDO_PAGO && transaccionAprobada != null) {
            transaccionService.registrarPagoAprobado(ordenId, transaccionAprobada, metodoPago);
            orden = transaccionService.obtenerOrden(ordenId);
        }

        if (orden.getEstado() == EstadoOrden.PAGO_CONFIRMADO) {
            try {
                ejecutarCompensacion(orden, motivo(error));
            } catch (RuntimeException errorCompensacion) {
                error.addSuppressed(errorCompensacion);
                log.error("No se pudo completar la compensación de la orden {} tras el fallo de la compra",
                        ordenId, errorCompensacion);
            }
        } else if (orden.getEstado() == EstadoOrden.PROCESANDO_PAGO) {
            transaccionService.revertirAEstadoReservado(ordenId);
        }
    }

    private void ejecutarCompensacion(OrdenCompra orden, String motivo) {
        if (orden.getTransaccionPagoId() == null || orden.getTransaccionPagoId().isBlank()) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "No se puede compensar una orden sin una transacción de pago");
        }

        ReembolsoResponse reembolso = Futures.join(pasarelaClient.reembolsarPago(
                new ReembolsoRequest(orden.getTransaccionPagoId(), orden.getMontoTotal())));
        if (reembolso == null || !"REEMBOLSADO".equalsIgnoreCase(reembolso.estado())) {
            String detalle = reembolso != null ? reembolso.mensaje() : "La pasarela no respondió";
            throw new NegocioException(HttpStatus.BAD_GATEWAY,
                    "No se pudo reembolsar la orden: " + detalle);
        }

        Futures.join(entradasClient.eliminarEntradasPorOrden(orden.getId().toString()));
        transaccionService.registrarOrdenCompensada(orden.getId(), motivo);
    }

    private String motivo(RuntimeException error) {
        return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
    }

    private ConfirmacionPagoResponse respuesta(OrdenCompra orden,
                                                List<EmisionResponse.EntradaDTO> entradas,
                                                String mensaje) {
        return new ConfirmacionPagoResponse(
                orden.getId(),
                orden.getEstado(),
                orden.getTransaccionPagoId(),
                orden.getMontoTotal(),
                orden.getMetodoPago(),
                entradas,
                mensaje);
    }
}
