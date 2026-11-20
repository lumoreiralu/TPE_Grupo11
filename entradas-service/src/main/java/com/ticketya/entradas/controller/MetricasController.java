package com.ticketya.entradas.controller;

import com.ticketya.entradas.dto.OcupacionResponse;
import com.ticketya.entradas.service.OcupacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reportes de ocupación por sector (R12.2). El acceso está restringido en
 * `api-gateway` a `ROLE_ADMINISTRADOR` y `ROLE_ORGANIZADOR`.
 */
@RestController
@RequestMapping("/api/entradas/metricas")
public class MetricasController {

    private final OcupacionService ocupacionService;

    public MetricasController(OcupacionService ocupacionService) {
        this.ocupacionService = ocupacionService;
    }

    @GetMapping("/{eventoId}/ocupacion")
    public ResponseEntity<OcupacionResponse> obtenerOcupacion(@PathVariable String eventoId) {
        return ResponseEntity.ok(ocupacionService.obtenerOcupacion(eventoId));
    }
}
