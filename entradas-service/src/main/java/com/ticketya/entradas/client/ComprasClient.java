package com.ticketya.entradas.client;

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

import com.ticketya.entradas.util.Futures;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Component
public class ComprasClient {

    private static final Logger log = LoggerFactory.getLogger(ComprasClient.class);

    private final WebClient webClient;

    public ComprasClient(@Qualifier("comprasWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @CircuitBreaker(name = "comprasService", fallbackMethod = "liberarAsientoFallback")
    @Retry(name = "comprasService")
    @TimeLimiter(name = "comprasService")
    public CompletableFuture<Void> liberarAsiento(String eventoId, String sectorId, String fila, Integer asiento) {
        return webClient.post()
                .uri("/api/compras/asientos/liberar")
                .bodyValue(new LiberarAsientoRequest(eventoId, sectorId, fila, asiento))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new ResponseStatusException(
                                HttpStatus.valueOf(response.statusCode().value()),
                                "compras-service rechazó la liberación del asiento")))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RemoteServiceException(
                                "compras-service devolvió " + response.statusCode().value())))
                .toBodilessEntity()
                .then()
                .toFuture();
    }

    private CompletableFuture<Void> liberarAsientoFallback(String eventoId, String sectorId,
                                                           String fila, Integer asiento, Throwable error) {
        Throwable causa = Futures.unwrap(error);
        if (causa instanceof ResponseStatusException responseStatusException) {
            return CompletableFuture.failedFuture(responseStatusException);
        }

        log.error("Fallback comprasService al liberar asiento {}|{}|{}|{}: {}",
                eventoId, sectorId, fila, asiento, causa.toString());
        return CompletableFuture.failedFuture(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "compras-service no responde; no se pudo liberar el asiento"));
    }

    /**
     * Obtiene los datos de pago de una orden para poder solicitar el reembolso
     * con la transacción y el monto que exige la pasarela.
     */
    @CircuitBreaker(name = "comprasService", fallbackMethod = "obtenerOrdenFallback")
    @Retry(name = "comprasService")
    @TimeLimiter(name = "comprasService")
    public CompletableFuture<OrdenInfo> obtenerOrden(String ordenId) {
        return webClient.get()
                .uri("/api/compras/{id}", ordenId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new ResponseStatusException(
                                HttpStatus.valueOf(response.statusCode().value()),
                                "compras-service no encontró la orden " + ordenId)))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RemoteServiceException(
                                "compras-service devolvió " + response.statusCode().value())))
                .bodyToMono(OrdenInfo.class)
                .toFuture();
    }

    private CompletableFuture<OrdenInfo> obtenerOrdenFallback(String ordenId, Throwable error) {
        Throwable causa = Futures.unwrap(error);
        if (causa instanceof ResponseStatusException responseStatusException) {
            return CompletableFuture.failedFuture(responseStatusException);
        }

        log.error("Fallback comprasService al consultar la orden {}: {}", ordenId, causa.toString());
        return CompletableFuture.failedFuture(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "compras-service no responde; no se pudo consultar la orden"));
    }

    public record LiberarAsientoRequest(String eventoId, String sectorId, String fila, Integer asiento) {
    }

    public record OrdenInfo(String transaccionPagoId, BigDecimal montoTotal) {
    }
}
