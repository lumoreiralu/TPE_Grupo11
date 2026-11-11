package ticketya.compras_service.dto;

import ticketya.compras_service.model.EstadoOrden;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReservaResponse(UUID ordenId,
                              EstadoOrden estado,
                              LocalDateTime expiraEn,
                              BigDecimal montoTotal,
                              long segundosRestantes) {}