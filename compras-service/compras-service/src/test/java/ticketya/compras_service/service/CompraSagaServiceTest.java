package ticketya.compras_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ticketya.compras_service.client.EmisionRequest;
import ticketya.compras_service.client.EmisionResponse;
import ticketya.compras_service.client.EntradasClient;
import ticketya.compras_service.client.PagoResponse;
import ticketya.compras_service.client.PasarelaClient;
import ticketya.compras_service.client.ReembolsoResponse;
import ticketya.compras_service.dto.ConfirmarPagoRequest;
import ticketya.compras_service.exception.NegocioException;
import ticketya.compras_service.messaging.DomainEvent;
import ticketya.compras_service.messaging.DomainEventPublisher;
import ticketya.compras_service.model.DetalleReserva;
import ticketya.compras_service.model.EstadoOrden;
import ticketya.compras_service.model.OrdenCompra;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraSagaServiceTest {

    @Mock
    private CompraTransaccionService transaccionService;
    @Mock
    private PasarelaClient pasarelaClient;
    @Mock
    private EntradasClient entradasClient;
    @Mock
    private DomainEventPublisher eventPublisher;
    @InjectMocks
    private CompraSagaService sagaService;

    @Test
    void fallaEmisionReembolsaLiberaAsientosYPublicaPagoCompensado() {
        UUID ordenId = UUID.randomUUID();
        OrdenCompra orden = ordenPagada(ordenId);
        when(transaccionService.iniciarProcesamientoPago(ordenId)).thenReturn(orden);
        when(transaccionService.obtenerOrden(ordenId)).thenReturn(orden, orden);
        doAnswer(invocation -> {
            orden.setEstado(EstadoOrden.PAGO_CONFIRMADO);
            orden.setTransaccionPagoId(invocation.getArgument(1));
            return null;
        }).when(transaccionService).registrarPagoAprobado(eq(ordenId), anyString(), anyString());
        when(pasarelaClient.procesarPago(any(), eq("idem-orden-1")))
                .thenReturn(CompletableFuture.completedFuture(
                        new PagoResponse("txn-1", "APROBADO", "aprobado")));
        when(entradasClient.emitirEntradas(any(EmisionRequest.class), eq("idem-orden-1")))
                .thenReturn(CompletableFuture.failedFuture(new NegocioException(
                        org.springframework.http.HttpStatus.BAD_GATEWAY, "falló la emisión")));
        when(pasarelaClient.reembolsarPago(any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new ReembolsoResponse("refund-1", "REEMBOLSADO", "reintegrado")));
        when(entradasClient.eliminarEntradasPorOrden(ordenId.toString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        assertThrows(NegocioException.class,
                () -> sagaService.confirmarPago(ordenId, new ConfirmarPagoRequest("TARJETA")));

        var compensacion = inOrder(pasarelaClient, entradasClient, transaccionService);
        compensacion.verify(pasarelaClient).reembolsarPago(any());
        compensacion.verify(entradasClient).eliminarEntradasPorOrden(ordenId.toString());
        compensacion.verify(transaccionService).registrarOrdenCompensada(
                eq(ordenId), anyString());

        ArgumentCaptor<DomainEvent> evento = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publish(evento.capture());
        org.junit.jupiter.api.Assertions.assertTrue(evento.getAllValues().stream()
                .anyMatch(publicado -> "pago.compensado".equals(publicado.type())));
    }

    @Test
    void reconciliacionDePagoAprobadoEmiteEntradasYCompletaLaOrden() {
        UUID ordenId = UUID.randomUUID();
        OrdenCompra orden = ordenPagada(ordenId);
        when(transaccionService.obtenerOrden(ordenId)).thenReturn(orden);
        when(pasarelaClient.consultarEstadoPagoPorOrden(ordenId.toString()))
                .thenReturn(CompletableFuture.completedFuture(
                        new PagoResponse("txn-reconciliado", "APROBADO", "aprobado")));
        doAnswer(invocation -> {
            orden.setEstado(EstadoOrden.PAGO_CONFIRMADO);
            orden.setTransaccionPagoId(invocation.getArgument(1));
            return null;
        }).when(transaccionService).registrarPagoAprobado(eq(ordenId), anyString(), anyString());
        when(entradasClient.emitirEntradas(any(EmisionRequest.class), eq("idem-orden-1")))
                .thenReturn(CompletableFuture.completedFuture(
                        new EmisionResponse(ordenId.toString(), List.of())));
        doAnswer(invocation -> {
            orden.setEstado(EstadoOrden.COMPLETADA);
            return null;
        }).when(transaccionService).registrarOrdenCompletada(ordenId);

        sagaService.reconciliarOrden(ordenId);

        verify(transaccionService).registrarOrdenCompletada(ordenId);
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.argThat(event ->
                "entrada.emitida".equals(event.type())));
        org.junit.jupiter.api.Assertions.assertEquals(EstadoOrden.COMPLETADA, orden.getEstado());
    }

    @Test
    void reconciliacionDePagoRechazadoCancelaLaOrden() {
        UUID ordenId = UUID.randomUUID();
        OrdenCompra orden = ordenPagada(ordenId);
        orden.setEstado(EstadoOrden.PROCESANDO_PAGO);
        when(transaccionService.obtenerOrden(ordenId)).thenReturn(orden);
        when(pasarelaClient.consultarEstadoPagoPorOrden(ordenId.toString()))
                .thenReturn(CompletableFuture.completedFuture(
                        new PagoResponse(null, "RECHAZADO", "rechazado")));
        doAnswer(invocation -> {
            orden.setEstado(EstadoOrden.CANCELADA);
            return null;
        }).when(transaccionService).registrarPagoRechazado(
                eq(ordenId), isNull(), anyString(), anyString());

        sagaService.reconciliarOrden(ordenId);

        verify(transaccionService).registrarPagoRechazado(
                eq(ordenId), isNull(), anyString(), anyString());
        org.junit.jupiter.api.Assertions.assertEquals(EstadoOrden.CANCELADA, orden.getEstado());
    }

    @Test
    void ordenYaCompletadaNoVuelveAProcesarPagoNiEmitirEntradas() {
        UUID ordenId = UUID.randomUUID();
        OrdenCompra orden = ordenPagada(ordenId);
        orden.setEstado(EstadoOrden.COMPLETADA);
        when(transaccionService.iniciarProcesamientoPago(ordenId)).thenReturn(orden);
        when(entradasClient.obtenerEntradasPorOrden(ordenId.toString()))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        sagaService.confirmarPago(ordenId, new ConfirmarPagoRequest("TARJETA"));

        verify(entradasClient).obtenerEntradasPorOrden(ordenId.toString());
        verify(pasarelaClient, org.mockito.Mockito.never()).procesarPago(any(), anyString());
        verify(entradasClient, org.mockito.Mockito.never()).emitirEntradas(any(), anyString());
    }

    private OrdenCompra ordenPagada(UUID id) {
        DetalleReserva detalle = new DetalleReserva();
        detalle.setSectorId("sector-1");
        detalle.setFila("A");
        detalle.setAsiento(1);
        detalle.setPrecioUnitario(new BigDecimal("100.00"));

        OrdenCompra orden = new OrdenCompra();
        orden.setId(id);
        orden.setClienteId("cliente-1");
        orden.setEventoId("evento-1");
        orden.setEstado(EstadoOrden.PROCESANDO_PAGO);
        orden.setMontoTotal(new BigDecimal("100.00"));
        orden.setIdempotencyKey("idem-orden-1");
        orden.setDetalles(List.of(detalle));
        return orden;
    }
}
