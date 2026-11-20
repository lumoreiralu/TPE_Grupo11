package ticketya.compras_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ticketya.compras_service.dto.RecaudacionResponse;
import ticketya.compras_service.service.MetricasService;

/**
 * Reportes de recaudación (R12.1). El acceso está restringido en
 * `api-gateway` a `ROLE_ADMINISTRADOR` y `ROLE_ORGANIZADOR`.
 */
@RestController
@RequestMapping("/api/compras/metricas")
public class MetricasController {

    private final MetricasService metricasService;

    public MetricasController(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    @GetMapping("/{eventoId}/recaudacion")
    public ResponseEntity<RecaudacionResponse> obtenerRecaudacion(@PathVariable String eventoId) {
        return ResponseEntity.ok(metricasService.obtenerRecaudacion(eventoId));
    }
}
