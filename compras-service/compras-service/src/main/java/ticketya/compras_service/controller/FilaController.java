package ticketya.compras_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ticketya.compras_service.client.EventoDTO;
import ticketya.compras_service.client.EventosClient;
import ticketya.compras_service.dto.*;
import ticketya.compras_service.service.FilaVirtualService;
import ticketya.compras_service.util.Futures;

@RestController
@RequestMapping("/api/fila")
public class FilaController {

    private final FilaVirtualService filaVirtualService;
    private final EventosClient eventosClient;

    public FilaController(FilaVirtualService filaVirtualService, EventosClient eventosClient) {
        this.filaVirtualService = filaVirtualService;
        this.eventosClient = eventosClient;
    }

    @PostMapping("/join")
    public ResponseEntity<FilaJoinResponse> join(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestBody FilaJoinRequest request) {
        String clienteId = resolverClienteId(xUserId, request != null ? request.clienteId() : null);
        String eventoId = request != null ? request.eventoId() : null;
        EventoDTO evento = obtenerEventoSiExiste(eventoId);
        return ResponseEntity.ok(filaVirtualService.join(eventoId, clienteId, evento));
    }

    @GetMapping("/turno")
    public ResponseEntity<FilaTurnoResponse> turno(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestParam(required = false) String eventoId,
            @RequestParam(required = false) String clienteId) {
        String cliente = resolverClienteId(xUserId, clienteId);
        return ResponseEntity.ok(filaVirtualService.turno(eventoId, cliente));
    }

    @PostMapping("/validar")
    public ResponseEntity<FilaValidarResponse> validar(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestBody FilaValidarRequest request) {
        String clienteId = resolverClienteId(xUserId, request != null ? request.clienteId() : null);
        String eventoId = request != null ? request.eventoId() : null;
        String paseId = request != null ? request.paseId() : null;
        return ResponseEntity.ok(filaVirtualService.validar(paseId, eventoId, clienteId));
    }

    @GetMapping("/estado")
    public ResponseEntity<FilaEstadoResponse> estado(@RequestParam String eventoId) {
        EventoDTO evento = obtenerEventoSiExiste(eventoId);
        return ResponseEntity.ok(filaVirtualService.estado(eventoId, evento));
    }

    private EventoDTO obtenerEventoSiExiste(String eventoId) {
        if (eventoId == null || eventoId.isBlank()) {
            return null;
        }
        return Futures.join(eventosClient.obtenerEvento(eventoId));
    }

    private String resolverClienteId(String xUserId, String bodyClienteId) {
        if (xUserId != null && !xUserId.isBlank()) return xUserId;
        return bodyClienteId;
    }
}
