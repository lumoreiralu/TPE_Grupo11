package ticketya.pasarela_mock.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ticketya.pasarela_mock.dto.PagoRequest;
import ticketya.pasarela_mock.dto.PagoResponse;
import ticketya.pasarela_mock.dto.ReembolsoRequest;
import ticketya.pasarela_mock.dto.ReembolsoResponse;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PagoService {

    public PagoResponse procesarPago(PagoRequest request) {
        String transaccionId = UUID.randomUUID().toString();

        if (request == null) {
            return new PagoResponse(transaccionId, "RECHAZADO", "Pago rechazado: solicitud vacía");
        }

        String metodo = request.getMetodoPago() != null ? request.getMetodoPago().toUpperCase() : "";

        // Simulación de fallo técnico / error 500
        if (metodo.contains("ERROR") || metodo.contains("TIMEOUT")) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error de comunicación interna con la red de pagos bancarios");
        }

        // Simulación de rechazo por fondos insuficientes o tarjeta denegada
        if (metodo.contains("RECHAZ") || metodo.contains("FONDOS") || metodo.contains("DENEGA")) {
            return new PagoResponse(transaccionId, "RECHAZADO", "Pago rechazado: fondos insuficientes o tarjeta denegada");
        }

        // Simulación de rechazo por tarjeta inválida o vencida
        if (metodo.contains("INVALIDA") || metodo.contains("VENCIDA")) {
            return new PagoResponse(transaccionId, "RECHAZADO", "Pago rechazado: tarjeta inválida o fecha de expiración vencida");
        }

        // Rechazo por monto nulo, cero o negativo
        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            return new PagoResponse(transaccionId, "RECHAZADO", "Pago rechazado: monto inválido");
        }

        return new PagoResponse(transaccionId, "APROBADO", "Pago aprobado exitosamente");
    }

    public ReembolsoResponse reembolsarPago(ReembolsoRequest request) {
        String reembolsoId = UUID.randomUUID().toString();

        if (request == null || request.getTransaccionId() == null || request.getTransaccionId().isBlank()) {
            return new ReembolsoResponse(reembolsoId, "RECHAZADO", "Reembolso rechazado: transaccionId requerida");
        }

        if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            return new ReembolsoResponse(reembolsoId, "RECHAZADO", "Reembolso rechazado: monto inválido");
        }

        return new ReembolsoResponse(reembolsoId, "REEMBOLSADO", "Reintegro procesado exitosamente a la cuenta de origen");
    }
}
