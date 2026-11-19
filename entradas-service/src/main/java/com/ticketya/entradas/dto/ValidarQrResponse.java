package com.ticketya.entradas.dto;

import com.ticketya.entradas.model.EstadoEntrada;
import java.time.LocalDateTime;
import java.util.UUID;

public record ValidarQrResponse(
    UUID entradaId,
    String clienteId,
    String eventoId,
    String sectorNombre,
    EstadoEntrada estado,
    LocalDateTime fechaIngreso,
    String mensaje
) {}
