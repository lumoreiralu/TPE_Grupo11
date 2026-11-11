package ticketya.compras_service.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Map<String, Object>> negocio(NegocioException e) {
        return ResponseEntity.status(e.getStatus())
                .body(Map.of("status", e.getStatus().value(), "mensaje", e.getMessage()));
    }

    // Red de seguridad: cualquier choque contra una restriccion unica de la base
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> conflicto(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("status", 409, "mensaje", "Conflicto: el recurso ya fue tomado o la solicitud esta duplicada"));
    }
}