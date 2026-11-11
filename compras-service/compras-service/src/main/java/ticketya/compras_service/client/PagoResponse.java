package ticketya.compras_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PagoResponse(String transaccionId, String estado, String mensaje) {}

