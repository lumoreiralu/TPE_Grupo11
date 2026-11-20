package com.ticketya.entradas.client;

import java.util.List;

/**
 * Vista mínima del evento que entradas-service necesita para calcular la
 * ocupación por sector (R12.2): la capacidad de cada sector vive en
 * eventos-service, no en la base de entradas.
 */
public record EventoResumenDTO(String id, String nombre, List<SectorDTO> sectores) {

    public record SectorDTO(String sectorId, String nombre, Integer capacidadMaxima) {
    }

    public List<SectorDTO> sectoresSeguros() {
        return sectores != null ? sectores : List.of();
    }
}
