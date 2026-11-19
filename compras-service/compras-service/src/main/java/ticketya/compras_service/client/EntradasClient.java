package ticketya.compras_service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ticketya.compras_service.exception.NegocioException;
import ticketya.compras_service.util.Futures;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class EntradasClient {

    private static final Logger log = LoggerFactory.getLogger(EntradasClient.class);

    private final WebClient webClient;

    public EntradasClient(@Qualifier("entradasWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @CircuitBreaker(name = "ticketService", fallbackMethod = "emitirEntradasFallback")
    @Retry(name = "ticketService")
    @TimeLimiter(name = "ticketService")
    public CompletableFuture<EmisionResponse> emitirEntradas(EmisionRequest request) {
        return webClient.post()
                .uri("/api/entradas/emitir")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    HttpStatus status = HttpStatus.valueOf(response.statusCode().value());
                    return Mono.error(new NegocioException(status,
                            "Error al comunicarse con entradas-service: " + status.value()));
                })
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RemoteServiceException(
                                "El servicio de entradas devolvió un error de servidor (5xx): "
                                        + response.statusCode().value())))
                .bodyToMono(EmisionResponse.class)
                .toFuture();
    }

    @CircuitBreaker(name = "ticketService", fallbackMethod = "obtenerEntradasPorOrdenFallback")
    @Retry(name = "ticketService")
    @TimeLimiter(name = "ticketService")
    public CompletableFuture<List<EmisionResponse.EntradaDTO>> obtenerEntradasPorOrden(String ordenCompraId) {
        return webClient.get()
                .uri("/api/entradas/orden/{ordenCompraId}", ordenCompraId)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RemoteServiceException(
                                "El servicio de entradas devolvió un error de servidor (5xx): "
                                        + response.statusCode().value())))
                .bodyToMono(new ParameterizedTypeReference<List<EmisionResponse.EntradaDTO>>() {
                })
                .defaultIfEmpty(List.of())
                .toFuture();
    }

    /**
     * Fallback de D4 para ticketService. La compensación completa de la saga
     * se incorpora en 4.4; por ahora se devuelve un error controlado para que
     * la orden quede en PAGO_CONFIRMADO y pueda reintentarse sin volver a cobrar.
     */
    private CompletableFuture<EmisionResponse> emitirEntradasFallback(EmisionRequest request, Throwable error) {
        Throwable causa = Futures.unwrap(error);
        if (causa instanceof NegocioException negocioException) {
            return CompletableFuture.failedFuture(negocioException);
        }

        log.error("Fallback ticketService al emitir entradas para la orden {}: {}",
                request != null ? request.ordenCompraId() : "desconocida", causa.toString());
        return CompletableFuture.failedFuture(new NegocioException(
                HttpStatus.BAD_GATEWAY,
                "entradas-service no pudo emitir las entradas; la orden queda pagada para reintentar"));
    }

    private CompletableFuture<List<EmisionResponse.EntradaDTO>> obtenerEntradasPorOrdenFallback(
            String ordenCompraId, Throwable error) {
        log.warn("Fallback ticketService al consultar entradas de la orden {}: {}",
                ordenCompraId, Futures.unwrap(error).toString());
        return CompletableFuture.completedFuture(List.of());
    }
}
