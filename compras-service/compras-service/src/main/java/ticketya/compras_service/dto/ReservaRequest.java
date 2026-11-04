package ticketya.compras_service.dto;

import java.util.List;

public record ReservaRequest(String clienteId, String eventoId, List<ItemReserva> items) {}