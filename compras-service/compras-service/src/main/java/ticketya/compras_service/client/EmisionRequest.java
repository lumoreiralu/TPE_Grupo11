package ticketya.compras_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmisionRequest(String ordenCompraId,
                              String clienteId,
                              String eventoId,
                              List<AsientoEntrada> asientos) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AsientoEntrada(String sectorNombre, String fila, Integer asiento) {}
}

