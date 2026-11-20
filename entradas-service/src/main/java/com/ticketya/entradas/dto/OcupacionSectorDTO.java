package com.ticketya.entradas.dto;

import java.math.BigDecimal;

/**
 * Ocupación de un sector (R12.2).
 *
 * <p>`vendidas` cuenta las entradas EMITIDA y USADA, `devueltas` las CANCELADA
 * (por devolución aprobada o cancelación del evento) y `disponibles` la
 * capacidad del sector menos las vendidas, sin bajar de cero.
 */
public record OcupacionSectorDTO(
        String sectorId,
        String sector,
        Integer capacidad,
        long vendidas,
        long disponibles,
        long devueltas,
        BigDecimal porcentajeOcupacion) {
}
