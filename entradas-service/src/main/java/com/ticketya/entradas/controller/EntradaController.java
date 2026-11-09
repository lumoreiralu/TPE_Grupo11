package com.ticketya.entradas.controller;

import com.ticketya.entradas.dto.EmisionEntradasRequest;
import com.ticketya.entradas.dto.EmisionEntradasResponse;
import com.ticketya.entradas.dto.EntradaResponseDTO;
import com.ticketya.entradas.service.EntradaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}

