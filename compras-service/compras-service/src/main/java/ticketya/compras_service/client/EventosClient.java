package ticketya.compras_service.client;

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
import reactor.core.publisher.Mono;
import ticketya.compras_service.exception.NegocioException;
import ticketya.compras_service.util.Futures;

import java.util.concurrent.CompletableFuture;

@Component
public class EventosClient {

    private static final Logger log = LoggerFactory.getLogger(EventosClient.class);

    private final WebClient webClient;

    public EventosClient(@Qualifier("eventosWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @CircuitBreaker(name = "eventService", fallbackMethod = "obtenerEventoFallback")
    @Retry(name = "eventService")
    @TimeLimiter(name = "eventService")
    public CompletableFuture<EventoDTO> obtenerEvento(String id) {
        return webClient.get()
                .uri("/api/eventos/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    HttpStatus status = HttpStatus.valueOf(response.statusCode().value());
                    String mensaje = status == HttpStatus.NOT_FOUND
                            ? "El evento " + id + " no existe"
                            : "Error al consultar eventos-service: " + status.value();
                    return Mono.error(new NegocioException(status, mensaje));
                })
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RemoteServiceException(
                                "eventos-service devolvió " + response.statusCode().value())))
                .bodyToMono(EventoDTO.class)
                .toFuture();
    }

    /**
     * Fallback de D4 para eventService: sin catálogo no se puede validar la
     * compra, así que se devuelve un error controlado en lugar de dejar la
     * orden a medias. Los errores de negocio 4xx se propagan sin alterar.
     */
    private CompletableFuture<EventoDTO> obtenerEventoFallback(String id, Throwable error) {
        Throwable causa = Futures.unwrap(error);
        if (causa instanceof NegocioException negocioException) {
            return CompletableFuture.failedFuture(negocioException);
        }

        log.warn("Fallback eventService al consultar el evento {}: {}", id, causa.toString());
        return CompletableFuture.failedFuture(new NegocioException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "eventos-service no responde; no se puede validar el evento en este momento"));
    }
}
