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

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Component
public class EventosClient {

    private static final Logger log = LoggerFactory.getLogger(EventosClient.class);

    private final WebClient webClient;

    public EventosClient(@Qualifier("eventosWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public record EventoFechaDTO(String id, LocalDateTime fechaHora) {
    }

    @CircuitBreaker(name = "eventService", fallbackMethod = "obtenerFechaEventoFallback")
    @Retry(name = "eventService")
    @TimeLimiter(name = "eventService")
    public CompletableFuture<LocalDateTime> obtenerFechaEvento(String eventoId) {
        return webClient.get()
                .uri("/api/eventos/{id}", eventoId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new ResponseStatusException(
                                HttpStatus.valueOf(response.statusCode().value()),
                                "No se pudo consultar el evento " + eventoId)))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RemoteServiceException(
                                "eventos-service devolvió " + response.statusCode().value())))
                .bodyToMono(EventoFechaDTO.class)
                .map(EventoFechaDTO::fechaHora)
                .toFuture();
    }

    private CompletableFuture<LocalDateTime> obtenerFechaEventoFallback(String eventoId, Throwable error) {
        Throwable causa = Futures.unwrap(error);
        if (causa instanceof ResponseStatusException responseStatusException) {
            return CompletableFuture.failedFuture(responseStatusException);
        }

        log.warn("Fallback eventService al consultar la fecha del evento {}: {}", eventoId, causa.toString());
        return CompletableFuture.failedFuture(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "eventos-service no responde; no se pudo verificar la fecha del evento"));
    }
}
