package ticketya.api_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Fallback de D4 para el catálogo: cuando eventService abre el circuito se
 * devuelve una lista vacía y un mensaje claro en lugar de propagar el error.
 */
@RestController
public class EventosFallbackController {

    @RequestMapping("/fallback/eventos")
    public ResponseEntity<Map<String, Object>> catalogoNoDisponible() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "mensaje", "Servicio temporalmente no disponible",
                        "eventos", List.of()
                ));
    }
}
