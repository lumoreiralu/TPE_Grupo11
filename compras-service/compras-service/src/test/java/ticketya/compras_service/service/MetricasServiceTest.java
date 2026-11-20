package ticketya.compras_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ticketya.compras_service.dto.RecaudacionResponse;
import ticketya.compras_service.exception.NegocioException;
import ticketya.compras_service.model.EstadoOrden;
import ticketya.compras_service.repository.OrdenCompraRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricasServiceTest {

    private static final String EVENTO = "evento-1";

    @Mock
    private OrdenCompraRepository ordenRepo;

    @Test
    void recaudacionCoincideConLasOrdenesPagadas() {
        when(ordenRepo.sumarMontoPorEventoYEstados(eq(EVENTO), eq(List.of(
                EstadoOrden.PAGO_CONFIRMADO, EstadoOrden.COMPLETADA, EstadoOrden.COMPENSADA))))
                .thenReturn(new BigDecimal("15000.00"));
        when(ordenRepo.sumarMontoPorEventoYEstados(eq(EVENTO), eq(List.of(EstadoOrden.COMPENSADA))))
                .thenReturn(new BigDecimal("1000.00"));
        when(ordenRepo.contarPorEventoYEstados(eq(EVENTO), eq(List.of(
                EstadoOrden.PAGO_CONFIRMADO, EstadoOrden.COMPLETADA))))
                .thenReturn(3L);
        when(ordenRepo.contarPorEventoYEstados(eq(EVENTO), eq(List.of(EstadoOrden.COMPENSADA))))
                .thenReturn(1L);

        RecaudacionResponse recaudacion = new MetricasService(ordenRepo).obtenerRecaudacion(EVENTO);

        assertEquals(EVENTO, recaudacion.eventoId());
        assertEquals(new BigDecimal("15000.00"), recaudacion.recaudacionBruta());
        assertEquals(new BigDecimal("1000.00"), recaudacion.montoReembolsado());
        assertEquals(new BigDecimal("14000.00"), recaudacion.recaudacionTotal());
        assertEquals(3L, recaudacion.ordenesConfirmadas());
        assertEquals(1L, recaudacion.ordenesCompensadas());
    }

    @Test
    void eventoSinOrdenesDevuelveRecaudacionCero() {
        when(ordenRepo.sumarMontoPorEventoYEstados(eq(EVENTO), eq(List.of(
                EstadoOrden.PAGO_CONFIRMADO, EstadoOrden.COMPLETADA, EstadoOrden.COMPENSADA))))
                .thenReturn(null);
        when(ordenRepo.sumarMontoPorEventoYEstados(eq(EVENTO), eq(List.of(EstadoOrden.COMPENSADA))))
                .thenReturn(null);
        when(ordenRepo.contarPorEventoYEstados(eq(EVENTO), eq(List.of(
                EstadoOrden.PAGO_CONFIRMADO, EstadoOrden.COMPLETADA))))
                .thenReturn(0L);
        when(ordenRepo.contarPorEventoYEstados(eq(EVENTO), eq(List.of(EstadoOrden.COMPENSADA))))
                .thenReturn(0L);

        RecaudacionResponse recaudacion = new MetricasService(ordenRepo).obtenerRecaudacion(EVENTO);

        assertEquals(BigDecimal.ZERO, recaudacion.recaudacionTotal());
        assertEquals(BigDecimal.ZERO, recaudacion.recaudacionBruta());
        assertEquals(BigDecimal.ZERO, recaudacion.montoReembolsado());
        assertEquals(0L, recaudacion.ordenesConfirmadas());
    }

    @Test
    void eventoVacioSeRechazaCon400() {
        NegocioException error = assertThrows(NegocioException.class,
                () -> new MetricasService(ordenRepo).obtenerRecaudacion("  "));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertTrue(error.getMessage().contains("evento"));
        verify(ordenRepo, never()).sumarMontoPorEventoYEstados(anyString(), anyList());
    }
}
