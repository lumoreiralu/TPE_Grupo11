package ticketya.eventos_service.dto;

import ticketya.eventos_service.model.Evento;

public record RecomendacionResponse(
        Evento evento,
        String motivo,
        int puntaje
) {}
