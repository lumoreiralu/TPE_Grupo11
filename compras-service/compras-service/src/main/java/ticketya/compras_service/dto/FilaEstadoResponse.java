package ticketya.compras_service.dto;

public record FilaEstadoResponse(
        String eventoId,
        String modoFila,
        int umbralConcurrencia,
        long enCola,
        long activos
) {}
