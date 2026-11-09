package ticketya.compras_service.dto;

import ticketya.compras_service.client.EmisionResponse.EntradaDTO;
import ticketya.compras_service.model.EstadoOrden;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ConfirmacionPagoResponse(
        UUID ordenId,
        EstadoOrden estado,
        String transaccionPagoId,
        BigDecimal montoTotal,
        String metodoPago,
        List<EntradaDTO> entradas,
        String mensaje
) {}

