package ticketya.compras_service.dto;

import java.math.BigDecimal;

/**
 * Reporte de recaudación acumulada de un evento (R12.1).
 *
 * <p>`recaudacionBruta` suma las órdenes con pago aprobado
 * (PAGO_CONFIRMADO, COMPLETADA y COMPENSADA, porque una orden compensada
 * primero tuvo el dinero cobrado). `montoReembolsado` descuenta las órdenes
 * compensadas/reembolsadas y `recaudacionTotal` es el neto que queda en caja.
 */
public record RecaudacionResponse(
        String eventoId,
        BigDecimal recaudacionTotal,
        BigDecimal recaudacionBruta,
        BigDecimal montoReembolsado,
        long ordenesConfirmadas,
        long ordenesCompensadas) {
}
