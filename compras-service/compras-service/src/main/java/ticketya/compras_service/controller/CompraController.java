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
import ticketya.compras_service.service.ReservaService;

import java.util.UUID;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final ReservaService reservaService;
    private BloqueoAsientoRepository bloqueoAsientoRepo;

    public CompraController(ReservaService reservaService, BloqueoAsientoRepository bloqueoAsientoRepo) {
        this.reservaService = reservaService;
        this.bloqueoAsientoRepo = bloqueoAsientoRepo;
    }

    @PostMapping("/reservar")
    public ResponseEntity<ReservaResponse> reservar(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ReservaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservaService.reservar(request, idempotencyKey));
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

    public record LiberarAsientoRequest(String eventoId, String sectorId, String fila, Integer asiento){}

    @PostMapping("/asientos/liberar")
    @Transactional
    public ResponseEntity<Void> liberarAsiento(@RequestBody LiberarAsientoRequest req){
        bloqueoAsientoRepo.deleteByEventoIdAndSectorIdAndFilaAndAsiento(req.eventoId(), req.sectorId(), req.fila(), req.asiento());
        return ResponseEntity.ok().build();
    }
}
