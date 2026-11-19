package ticketya.compras_service.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import ticketya.compras_service.exception.NegocioException;
import ticketya.compras_service.util.Futures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Comprueba que las anotaciones de Resilience4j realmente ejecutan el fallback
 * de D4 cuando la dependencia remota no responde.
 */
@SpringBootTest
class ClienteResilienciaTest {

    @Autowired
    private EventosClient eventosClient;

    @Test
    @DisplayName("eventos-service caído devuelve un error controlado 503 en vez de propagar la excepción de red")
    void dependenciaCaidaUsaFallbackControlado() {
        NegocioException error = assertThrows(NegocioException.class,
                () -> Futures.join(eventosClient.obtenerEvento("evt-caido")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
    }
}
