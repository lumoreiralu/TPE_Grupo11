package ticketya.compras_service.dto;

import java.util.List;

public record ReservaRequest(String clienteId, String eventoId, List<ItemReserva> items, String paseId) {
    public ReservaRequest(String clienteId, String eventoId, List<ItemReserva> items) {
        this(clienteId, eventoId, items, null);
    }
}