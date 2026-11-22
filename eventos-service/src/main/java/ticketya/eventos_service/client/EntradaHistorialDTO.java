package ticketya.eventos_service.client;

import java.time.LocalDateTime;

public record EntradaHistorialDTO(
        String eventoId,
        String clienteId,
        String estado,
        LocalDateTime fechaEmision
) {}
