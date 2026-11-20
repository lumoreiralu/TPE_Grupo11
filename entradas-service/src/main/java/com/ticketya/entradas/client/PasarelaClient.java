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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class PasarelaClient {

    private static final Logger log = LoggerFactory.getLogger(PasarelaClient.class);

    private final WebClient webClient;

    public PasarelaClient(@Qualifier("pasarelaWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Solicita el reintegro de una orden. La {@code Idempotency-Key} debe ser
     * determinística (por ejemplo {@code eventoId + ordenId}) para que la
     * pasarela no procese dos veces el mismo reembolso.
     */
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "solicitarReembolsoFallback")
    @Retry(name = "paymentGateway")
    @TimeLimiter(name = "paymentGateway")
    public CompletableFuture<Void> solicitarReembolso(String transaccionId, BigDecimal monto,
                                                      String idempotencyKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transaccionId", transaccionId);
        body.put("monto", monto);

        WebClient.RequestHeadersSpec<?> request = webClient.post()
                .uri("/api/pagos/reembolsar")
                .bodyValue(body);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            request = request.header("Idempotency-Key", idempotencyKey);
        }

        return request.retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new ResponseStatusException(
                                HttpStatus.valueOf(response.statusCode().value()),
                                "La pasarela rechazó el reembolso de la transacción " + transaccionId)))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RemoteServiceException(
                                "La pasarela devolvió " + response.statusCode().value())))
                .toBodilessEntity()
                .then()
                .toFuture();
    }

    /**
     * Si la pasarela no responde se propaga el fallo para que la devolución no
     * quede marcada como aprobada sin reintegro. En la cancelación masiva cada
     * orden se maneja de forma aislada y el error se registra.
     */
    private CompletableFuture<Void> solicitarReembolsoFallback(String transaccionId, BigDecimal monto,
                                                                String idempotencyKey, Throwable error) {
        Throwable causa = Futures.unwrap(error);
        if (causa instanceof ResponseStatusException responseStatusException) {
            return CompletableFuture.failedFuture(responseStatusException);
        }
        log.error("Fallback paymentGateway al reembolsar la transacción {}: {}",
                transaccionId, causa.toString());
        return CompletableFuture.failedFuture(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "La pasarela no pudo completar el reembolso de la transacción " + transaccionId));
    }
}
