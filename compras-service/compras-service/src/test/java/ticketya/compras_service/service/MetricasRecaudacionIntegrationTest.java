package ticketya.compras_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ticketya.compras_service.dto.RecaudacionResponse;
import ticketya.compras_service.model.EstadoOrden;
import ticketya.compras_service.model.OrdenCompra;
import ticketya.compras_service.repository.OrdenCompraRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifica contra la base real (H2) que la recaudación coincide con las
 * órdenes pagadas y descuenta las compensadas (R12.1).
 */
@SpringBootTest
class MetricasRecaudacionIntegrationTest {

    private static final String EVENTO = "evento-metricas";
    private static final String OTRO_EVENTO = "evento-ajeno";

    @Autowired
    private MetricasService metricasService;

    @Autowired
    private OrdenCompraRepository ordenRepo;

    @BeforeEach
    void setUp() {
        ordenRepo.deleteAll();
    }

    @Test
    void recaudacionSumaOrdenesPagadasYDescuentaCompensadas() {
        guardarOrden(EVENTO, EstadoOrden.COMPLETADA, "5000.00");
        guardarOrden(EVENTO, EstadoOrden.COMPLETADA, "2500.50");
        guardarOrden(EVENTO, EstadoOrden.PAGO_CONFIRMADO, "1000.00");
        guardarOrden(EVENTO, EstadoOrden.COMPENSADA, "800.00");
        // Órdenes que nunca cobraron dinero: no deben sumar.
        guardarOrden(EVENTO, EstadoOrden.CANCELADA, "9999.00");
        guardarOrden(EVENTO, EstadoOrden.EXPIRADA, "9999.00");
        guardarOrden(EVENTO, EstadoOrden.RESERVADA_TEMPORAL, "9999.00");
        // Orden de otro evento: no debe mezclarse.
        guardarOrden(OTRO_EVENTO, EstadoOrden.COMPLETADA, "77777.00");

        RecaudacionResponse recaudacion = metricasService.obtenerRecaudacion(EVENTO);

        assertEquals(EVENTO, recaudacion.eventoId());
        assertEquals(new BigDecimal("9300.50"), recaudacion.recaudacionBruta());
        assertEquals(new BigDecimal("800.00"), recaudacion.montoReembolsado());
        assertEquals(new BigDecimal("8500.50"), recaudacion.recaudacionTotal());
        assertEquals(3L, recaudacion.ordenesConfirmadas());
        assertEquals(1L, recaudacion.ordenesCompensadas());
    }

    @Test
    void eventoSinVentasDevuelveCero() {
        RecaudacionResponse recaudacion = metricasService.obtenerRecaudacion("evento-sin-ventas");

        assertEquals(BigDecimal.ZERO, recaudacion.recaudacionTotal());
        assertEquals(BigDecimal.ZERO, recaudacion.recaudacionBruta());
        assertEquals(BigDecimal.ZERO, recaudacion.montoReembolsado());
        assertEquals(0L, recaudacion.ordenesConfirmadas());
        assertEquals(0L, recaudacion.ordenesCompensadas());
    }

    private void guardarOrden(String eventoId, EstadoOrden estado, String monto) {
        OrdenCompra orden = new OrdenCompra();
        orden.setClienteId("cliente-metricas");
        orden.setEventoId(eventoId);
        orden.setEstado(estado);
        orden.setMontoTotal(new BigDecimal(monto));
        orden.setFechaCreacion(LocalDateTime.now());
        orden.setFechaExpiracionReserva(LocalDateTime.now().plusMinutes(10));
        if (estado == EstadoOrden.COMPENSADA) {
            orden.setMotivoCompensacion("Compensación de prueba");
        }
        ordenRepo.saveAndFlush(orden);
    }
}
