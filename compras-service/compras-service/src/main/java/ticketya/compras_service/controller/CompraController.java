package ticketya.compras_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ticketya.compras_service.dto.ConfirmacionPagoResponse;
import ticketya.compras_service.dto.ConfirmarPagoRequest;
import ticketya.compras_service.dto.ReservaRequest;
import ticketya.compras_service.dto.ReservaResponse;
import ticketya.compras_service.repository.BloqueoAsientoRepository;
import ticketya.compras_service.service.CompraSagaService;
import ticketya.compras_service.service.ReservaService;

import java.util.UUID;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final ReservaService reservaService;
    private final CompraSagaService compraSagaService;
    private BloqueoAsientoRepository bloqueoAsientoRepo;

    public CompraController(ReservaService reservaService,
                            CompraSagaService compraSagaService,
                            BloqueoAsientoRepository bloqueoAsientoRepo) {
        this.reservaService = reservaService;
        this.compraSagaService = compraSagaService;
        this.bloqueoAsientoRepo = bloqueoAsientoRepo;
    }

    @PostMapping("/reservar")
    public ResponseEntity<ReservaResponse> reservar(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-Pase-Id", required = false) String paseHeader,
            @RequestBody ReservaRequest request) {
        String paseId = request.paseId() != null && !request.paseId().isBlank()
                ? request.paseId()
                : paseHeader;
        // If gateway propagated X-User-Id, use it as clienteId (overrides body value)
        ReservaRequest effectiveRequest = (xUserId != null && !xUserId.isBlank())
                ? new ReservaRequest(xUserId, request.eventoId(), request.items(), paseId)
                : new ReservaRequest(request.clienteId(), request.eventoId(), request.items(), paseId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservaService.reservar(effectiveRequest, idempotencyKey));
    }

    @PostMapping("/{id}/confirmar-pago")
    public ResponseEntity<ConfirmacionPagoResponse> confirmarPago(
            @PathVariable UUID id,
            @RequestBody(required = false) ConfirmarPagoRequest request) {
        return ResponseEntity.ok(reservaService.confirmarPago(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConfirmacionPagoResponse> consultarOrden(@PathVariable UUID id) {
        return ResponseEntity.ok(reservaService.consultarOrden(id));
    }

    @PostMapping("/{id}/compensar")
    public ResponseEntity<ConfirmacionPagoResponse> compensar(@PathVariable UUID id) {
        return ResponseEntity.ok(compraSagaService.compensarManualmente(id));
    }

    public record LiberarAsientoRequest(String eventoId, String sectorId, String fila, Integer asiento){}

    @PostMapping("/asientos/liberar")
    @Transactional
    public ResponseEntity<Void> liberarAsiento(@RequestBody LiberarAsientoRequest req){
        String claveAsiento = req.eventoId() + "|" + req.sectorId() + "|" + req.fila() + "|" + req.asiento();
        bloqueoAsientoRepo.deleteByClaveAsiento(claveAsiento);
        return ResponseEntity.ok().build();
    }
}
