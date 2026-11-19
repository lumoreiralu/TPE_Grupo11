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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class PasarelaClient {

    private static final Logger log = LoggerFactory.getLogger(PasarelaClient.class);

    private final WebClient webClient;

    public PasarelaClient(@Qualifier("pasarelaWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "solicitarReembolsoFallback")
    @Retry(name = "paymentGateway")
    @TimeLimiter(name = "paymentGateway")
    public CompletableFuture<Void> solicitarReembolso(String ordenCompraId, String clienteId) {
        return webClient.post()
                .uri("/api/pagos/reembolsar")
                .bodyValue(Map.of("ordenId", ordenCompraId, "clienteId", clienteId))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new ResponseStatusException(
                                HttpStatus.valueOf(response.statusCode().value()),
                                "La pasarela rechazó el reembolso de la orden " + ordenCompraId)))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RemoteServiceException(
                                "La pasarela devolvió " + response.statusCode().value())))
                .toBodilessEntity()
                .then()
                .toFuture();
    }

    /**
     * Fallback de D4 para paymentGateway en el flujo de reembolsos: se registra
     * el aviso y se continúa, igual que el comportamiento previo de este
     * cliente best-effort. La compensación explícita llega en 4.5-4.6.
     */
    private CompletableFuture<Void> solicitarReembolsoFallback(String ordenCompraId, String clienteId,
                                                                Throwable error) {
        log.warn("Fallback paymentGateway al solicitar reembolso de la orden {}: {}",
                ordenCompraId, Futures.unwrap(error).toString());
        return CompletableFuture.completedFuture(null);
    }
}
