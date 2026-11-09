package com.ticketya.entradas.dto;

import com.ticketya.entradas.model.EstadoEntrada;

import java.time.LocalDateTime;
import java.util.UUID;

public record EntradaResponseDTO(
        UUID id,
        String ordenCompraId,
        String clienteId,
        String eventoId,
        String sectorNombre,
        String fila,
        Integer asiento,
        String codigoQrHash,
        EstadoEntrada estado,
        LocalDateTime fechaEmision
) {}

