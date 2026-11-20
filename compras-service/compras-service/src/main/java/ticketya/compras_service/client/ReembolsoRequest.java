package ticketya.compras_service.client;

import java.math.BigDecimal;

public record ReembolsoRequest(String transaccionId, BigDecimal monto) {}
