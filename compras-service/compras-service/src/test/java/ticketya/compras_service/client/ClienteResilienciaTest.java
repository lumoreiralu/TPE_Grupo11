package ticketya.compras_service.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import ticketya.compras_service.exception.NegocioException;
import ticketya.compras_service.util.Futures;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "resilience4j.circuitbreaker.instances.paymentGateway.sliding-window-size=2",
        "resilience4j.circuitbreaker.instances.paymentGateway.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.instances.paymentGateway.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.paymentGateway.wait-duration-in-open-state=30s",
        "resilience4j.circuitbreaker.instances.ticketService.sliding-window-size=2",
        "resilience4j.circuitbreaker.instances.ticketService.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.instances.ticketService.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.ticketService.wait-duration-in-open-state=30s",
        "resilience4j.circuitbreaker.instances.eventService.sliding-window-size=2",
        "resilience4j.circuitbreaker.instances.eventService.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.instances.eventService.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.eventService.wait-duration-in-open-state=30s",
        "resilience4j.retry.instances.paymentGateway.max-attempts=1",
        "resilience4j.retry.instances.ticketService.max-attempts=1",
        "resilience4j.retry.instances.eventService.max-attempts=1"
})
class ClienteResilienciaTest {

    @Autowired
    private EventosClient eventosClient;
    @Autowired
    private EntradasClient entradasClient;
    @Autowired
    private PasarelaClient pasarelaClient;
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetearCircuitBreakers() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }

    @Test
    @DisplayName("fallos repetidos abren los circuitos y usan los fallbacks D4 de cada cliente")
    void dependenciasCaidasAbrenCircuitosYUsanFallbacksControlados() {
        verificarFallbackAlAbrirCircuito("eventService", () ->
                        Futures.join(eventosClient.obtenerEvento("evt-caido")),
                HttpStatus.SERVICE_UNAVAILABLE, "no responde");

        verificarFallbackAlAbrirCircuito("ticketService", () ->
                        Futures.join(entradasClient.emitirEntradas(
                                new EmisionRequest("orden-1", "cliente-1", "evento-1", List.of()),
                                "idem-1")),
                HttpStatus.BAD_GATEWAY, "saga gestionará la compensación");

        verificarFallbackAlAbrirCircuito("paymentGateway", () ->
                        Futures.join(pasarelaClient.procesarPago(
                                new PagoRequest(new BigDecimal("100"), "orden-1", "TARJETA"), "idem-1")),
                HttpStatus.SERVICE_UNAVAILABLE, "permanece pendiente");
    }

    private void verificarFallbackAlAbrirCircuito(String nombre, Runnable llamada,
                                                    HttpStatus statusEsperado, String mensajeEsperado) {
        for (int intento = 0; intento < 2; intento++) {
            NegocioException error = assertThrows(NegocioException.class, llamada::run);
            assertEquals(statusEsperado, error.getStatus());
            assertTrue(error.getMessage().contains(mensajeEsperado));
        }

        assertEquals(CircuitBreaker.State.OPEN,
                circuitBreakerRegistry.circuitBreaker(nombre).getState());

        NegocioException fallback = assertThrows(NegocioException.class, llamada::run);
        assertEquals(statusEsperado, fallback.getStatus());
        assertTrue(fallback.getMessage().contains(mensajeEsperado));
    }
}
