package ticketya.compras_service.dto;

public record FilaTurnoResponse(
        String eventoId,
        String clienteId,
        long posicion,
        boolean turnoActivo,
        String paseId,
        Long segundosRestantesPase
) {}
