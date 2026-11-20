package com.ticketya.entradas.controller;

import com.ticketya.entradas.dto.EmisionEntradasRequest;
import com.ticketya.entradas.dto.EmisionEntradasResponse;
import com.ticketya.entradas.dto.EntradaResponseDTO;
import com.ticketya.entradas.dto.ValidarQrRequest;
import com.ticketya.entradas.dto.ValidarQrResponse;
import com.ticketya.entradas.model.SolicitudDevolucion;
import com.ticketya.entradas.service.EntradaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/entradas")
public class EntradaController {

    private final EntradaService entradaService;

    public EntradaController(EntradaService entradaService) {
        this.entradaService = entradaService;
    }

    @PostMapping("/emitir")
    public ResponseEntity<EmisionEntradasResponse> emitirEntradas(@RequestBody EmisionEntradasRequest request) {
        EmisionEntradasResponse response = entradaService.emitirEntradas(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/orden/{ordenCompraId}")
    public ResponseEntity<List<EntradaResponseDTO>> obtenerPorOrden(@PathVariable String ordenCompraId) {
        List<EntradaResponseDTO> entradas = entradaService.obtenerPorOrden(ordenCompraId);
        return ResponseEntity.ok(entradas);
    }

    @DeleteMapping("/orden/{ordenCompraId}")
    public ResponseEntity<Void> eliminarPorOrden(@PathVariable String ordenCompraId) {
        entradaService.eliminarEntradasPorOrden(ordenCompraId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/devolucion")
    public ResponseEntity<SolicitudDevolucion> solicitarDevolucion(@PathVariable UUID id, @RequestHeader(value = "X-User-Id", required = false) String xUserId, @RequestParam(required = false) String clienteId, @RequestParam(required = false) String motivo){
        String resolvedClienteId = (xUserId != null) ? xUserId : clienteId;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entradaService.solicitarDevolucion(id, resolvedClienteId, motivo));
    }

    @PostMapping("/devoluciones/{solicitudId}/aprobar")
    public ResponseEntity<Void> aprobarDevolucion(@PathVariable UUID solicitudId, @RequestParam(required = false, defaultValue = "Aprobada") String observacion){
        entradaService.aprobarDevolucion(solicitudId, observacion);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/eventos/{eventoId}/cancelar")
    public ResponseEntity<String> cancelarEntradasEvento(@PathVariable String eventoId){
        int canceladas = entradaService.cancelarEntradasPorEvento(eventoId);
        return ResponseEntity.ok("Se cancelaron" + canceladas + " entradas y se liberaron sus asientos");
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<EntradaResponseDTO>> obtenerPorCliente(@PathVariable String clienteId, @RequestHeader(value = "X-User-Id", required = false) String xUserId) {
        String resolvedClienteId = (xUserId != null) ? xUserId : clienteId;
        List<EntradaResponseDTO> entradas = entradaService.obtenerPorCliente(resolvedClienteId);
        return ResponseEntity.ok(entradas);
    }

    @PostMapping("/devoluciones/{solicitudId}/rechazar")
    public ResponseEntity<Void> rechazarDevolucion(
            @PathVariable UUID solicitudId,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String motivo = body != null ? body.get("motivo") : null;
        entradaService.rechazarDevolucion(solicitudId, motivo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/validar-qr")
    public ResponseEntity<ValidarQrResponse> validarQr(
            @RequestBody ValidarQrRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String validadorId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        return ResponseEntity.ok(entradaService.validarQr(request, validadorId, roles));
    }
}
