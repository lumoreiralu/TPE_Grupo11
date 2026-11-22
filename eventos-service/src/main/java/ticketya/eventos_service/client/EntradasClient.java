package ticketya.eventos_service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import ticketya.eventos_service.util.Futures;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class EntradasClient {

    private static final Logger log = LoggerFactory.getLogger(EntradasClient.class);

    private final WebClient webClient;

    public EntradasClient(@Qualifier("entradasWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @CircuitBreaker(name = "ticketService", fallbackMethod = "cancelarEntradasFallback")
    @Retry(name = "ticketService")
    @TimeLimiter(name = "ticketService")
    public CompletableFuture<Void> cancelarEntradasPorEvento(String eventoId) {
        return webClient.post()
                .uri("/api/entradas/eventos/{eventoId}/cancelar", eventoId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new ResponseStatusException(
                                HttpStatus.valueOf(response.statusCode().value()),
                                "entradas-service rechazó la cancelación del evento " + eventoId)))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RemoteServiceException(
                                "entradas-service devolvió " + response.statusCode().value())))
                .toBodilessEntity()
                .then()
                .toFuture();
    }

    @CircuitBreaker(name = "ticketService", fallbackMethod = "obtenerHistorialFallback")
    @Retry(name = "ticketService")
    @TimeLimiter(name = "ticketService")
    public CompletableFuture<List<EntradaHistorialDTO>> obtenerEntradasPorCliente(String clienteId) {
        return webClient.get()
                .uri("/api/entradas/cliente/{clienteId}", clienteId)
                .header("X-User-Id", clienteId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new ResponseStatusException(
                                HttpStatus.valueOf(response.statusCode().value()),
                                "entradas-service rechazó la consulta de entradas de " + clienteId)))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RemoteServiceException(
                                "entradas-service devolvió " + response.statusCode().value())))
                .bodyToFlux(EntradaHistorialDTO.class)
                .collectList()
                .toFuture();
    }

    private CompletableFuture<List<EntradaHistorialDTO>> obtenerHistorialFallback(
            String clienteId, Throwable error) {
        Throwable causa = Futures.unwrap(error);
        log.warn("Fallback ticketService al consultar el historial de {}: {}",
                clienteId, causa.toString());
        return CompletableFuture.completedFuture(List.of());
    }

    /**
     * Fallback de D4 para ticketService desde eventos-service: la cancelación
     * del evento ya quedó persistida y la notificación es best-effort, así que
     * se registra el aviso sin romper la respuesta. La compensación completa
     * de las órdenes se incorpora en 4.5-4.6.
     */
    private CompletableFuture<Void> cancelarEntradasFallback(String eventoId, Throwable error) {
        Throwable causa = Futures.unwrap(error);
        if (causa instanceof ResponseStatusException responseStatusException) {
            return CompletableFuture.failedFuture(responseStatusException);
        }

        log.error("Fallback ticketService al cancelar las entradas del evento {}: {}",
                eventoId, causa.toString());
        return CompletableFuture.completedFuture(null);
    }
}
