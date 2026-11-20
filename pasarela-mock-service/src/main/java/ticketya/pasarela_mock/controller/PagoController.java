package ticketya.pasarela_mock.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ticketya.pasarela_mock.dto.PagoRequest;
import ticketya.pasarela_mock.dto.PagoResponse;
import ticketya.pasarela_mock.dto.ReembolsoRequest;
import ticketya.pasarela_mock.dto.ReembolsoResponse;
import ticketya.pasarela_mock.service.PagoService;
import ticketya.pasarela_mock.service.RedisIdempotencyStore;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    @Autowired
    private PagoService pagoService;

    @Autowired
    private RedisIdempotencyStore idempotencyStore;

    @PostMapping("/procesar")
    public ResponseEntity<PagoResponse> procesarPago(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody PagoRequest request) {
        PagoResponse response = idempotencyStore.execute(
                "pago", idempotencyKey, PagoResponse.class, () -> pagoService.procesarPago(request));
        if (response.getTransaccionId() != null) {
            idempotencyStore.put("pago:estado:transaccion:" + response.getTransaccionId(), response);
        }
        if (request != null && request.getOrdenId() != null && !request.getOrdenId().isBlank()) {
            idempotencyStore.put("pago:estado:orden:" + request.getOrdenId(), response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/estado/{paymentId}")
    public ResponseEntity<PagoResponse> consultarEstado(@PathVariable String paymentId) {
        return ResponseEntity.ok(estado(idempotencyStore.get(
                "pago:estado:transaccion:" + paymentId, PagoResponse.class)));
    }

    @GetMapping("/estado/orden/{ordenId}")
    public ResponseEntity<PagoResponse> consultarEstadoPorOrden(@PathVariable String ordenId) {
        return ResponseEntity.ok(estado(idempotencyStore.get(
                "pago:estado:orden:" + ordenId, PagoResponse.class)));
    }

    private PagoResponse estado(PagoResponse response) {
        return response != null
                ? response
                : new PagoResponse(null, "NO_ENCONTRADO", "No se encontró un pago para la referencia indicada");
    }

    @PostMapping("/reembolsar")
    public ResponseEntity<ReembolsoResponse> reembolsarPago(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ReembolsoRequest request) {
        // La clave determinística evita reintegrar dos veces la misma orden
        // cuando se reintenta o se repite la cancelación de un evento.
        ReembolsoResponse response = idempotencyStore.execute(
                "reembolso", idempotencyKey, ReembolsoResponse.class,
                () -> pagoService.reembolsarPago(request));
        return ResponseEntity.ok(response);
    }
}
