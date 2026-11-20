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
public class PasarelaClient {

    private static final Logger log = LoggerFactory.getLogger(PasarelaClient.class);

    private final WebClient webClient;

    public PasarelaClient(@Qualifier("pasarelaWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "procesarPagoFallback")
    @Retry(name = "paymentGateway")
    @TimeLimiter(name = "paymentGateway")
    public CompletableFuture<PagoResponse> procesarPago(PagoRequest request) {
        return webClient.post()
                .uri("/api/pagos/procesar")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    HttpStatus status = HttpStatus.valueOf(response.statusCode().value());
                    return Mono.error(new NegocioException(status,
                            "Error al comunicarse con la pasarela de pagos: " + status.value()));
                })
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RemoteServiceException(
                                "La pasarela de pagos devolvió un error de servidor (5xx): "
                                        + response.statusCode().value())))
                .bodyToMono(PagoResponse.class)
                .toFuture();
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "reembolsarPagoFallback")
    @Retry(name = "paymentGateway")
    @TimeLimiter(name = "paymentGateway")
    public CompletableFuture<ReembolsoResponse> reembolsarPago(ReembolsoRequest request) {
        return webClient.post()
                .uri("/api/pagos/reembolsar")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    HttpStatus status = HttpStatus.valueOf(response.statusCode().value());
                    return Mono.error(new NegocioException(status,
                            "Error al comunicarse con la pasarela de pagos: " + status.value()));
                })
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RemoteServiceException(
                                "La pasarela de pagos devolvió un error de servidor (5xx): "
                                        + response.statusCode().value())))
                .bodyToMono(ReembolsoResponse.class)
                .toFuture();
    }

    /**
     * Fallback de D4 para paymentGateway: no se vuelve a cobrar. El estado
     * PAGO_PENDIENTE y su reconciliación se completan en la saga (4.4-4.7);
     * hasta entonces la orden se revierte a RESERVADA_TEMPORAL en el servicio.
     */
    private CompletableFuture<PagoResponse> procesarPagoFallback(PagoRequest request, Throwable error) {
        Throwable causa = Futures.unwrap(error);
        if (causa instanceof NegocioException negocioException) {
            return CompletableFuture.failedFuture(negocioException);
        }

        log.error("Fallback paymentGateway para la orden {}: {}",
                request != null ? request.ordenId() : "desconocida", causa.toString());
        return CompletableFuture.failedFuture(new NegocioException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "La pasarela de pagos no está disponible; no se realizó ningún cargo"));
    }

    private CompletableFuture<ReembolsoResponse> reembolsarPagoFallback(
            ReembolsoRequest request, Throwable error) {
        Throwable causa = Futures.unwrap(error);
        if (causa instanceof NegocioException negocioException) {
            return CompletableFuture.failedFuture(negocioException);
        }

        log.error("Fallback paymentGateway al reembolsar la transacción {}: {}",
                request != null ? request.transaccionId() : "desconocida", causa.toString());
        return CompletableFuture.failedFuture(new NegocioException(
                HttpStatus.BAD_GATEWAY, "La pasarela no pudo completar el reembolso"));
    }
}
