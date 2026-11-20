package ticketya.compras_service.dto;

public record FilaJoinResponse(
        String eventoId,
        String clienteId,
        long posicion,
        boolean turnoActivo,
        String paseId,
        Long segundosRestantesPase,
        String mensaje
) {}
