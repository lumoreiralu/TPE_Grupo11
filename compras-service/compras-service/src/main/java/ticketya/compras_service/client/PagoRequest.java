package ticketya.compras_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PagoRequest(BigDecimal monto, String ordenId, String metodoPago) {}

