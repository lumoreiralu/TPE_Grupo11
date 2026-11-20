package com.ticketya.entradas.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reporte de ocupación de un evento con el desglose por sector (R12.2).
 */
public record OcupacionResponse(
        String eventoId,
        String nombreEvento,
        List<OcupacionSectorDTO> sectores,
        long totalVendidas,
        long totalDisponibles,
        long totalDevueltas,
        BigDecimal porcentajeOcupacionTotal) {
}
