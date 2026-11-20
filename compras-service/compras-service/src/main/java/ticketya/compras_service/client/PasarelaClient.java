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
    public CompletableFuture<PagoResponse> procesarPago(PagoRequest request, String idempotencyKey) {
        return webClient.post()
                .uri("/api/pagos/procesar")
                .header("Idempotency-Key", idempotencyKey)
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

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "consultarEstadoPagoFallback")
    @Retry(name = "paymentGateway")
    @TimeLimiter(name = "paymentGateway")
    public CompletableFuture<PagoResponse> consultarEstadoPago(String paymentId) {
        return webClient.get()
                .uri("/api/pagos/estado/{paymentId}", paymentId)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new RemoteServiceException(
                                "La pasarela de pagos devolvió un error de servidor (5xx): "
                                        + response.statusCode().value())))
                .bodyToMono(PagoResponse.class)
                .toFuture();
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "consultarEstadoPagoPorOrdenFallback")
    @Retry(name = "paymentGateway")
    @TimeLimiter(name = "paymentGateway")
    public CompletableFuture<PagoResponse> consultarEstadoPagoPorOrden(String ordenId) {
        return webClient.get()
                .uri("/api/pagos/estado/orden/{ordenId}", ordenId)
                .retrieve()
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
     * Fallback de D4 para paymentGateway: no se vuelve a cobrar y se conserva
     * PROCESANDO_PAGO para que la reconciliación consulte el resultado real.
     */
    private CompletableFuture<PagoResponse> procesarPagoFallback(
            PagoRequest request, String idempotencyKey, Throwable error) {
        Throwable causa = Futures.unwrap(error);
        if (causa instanceof NegocioException negocioException) {
            return CompletableFuture.failedFuture(negocioException);
        }

        log.error("Fallback paymentGateway para la orden {}: {}",
                request != null ? request.ordenId() : "desconocida", causa.toString());
        return CompletableFuture.failedFuture(new NegocioException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo confirmar el resultado del pago; la orden permanece pendiente de reconciliación"));
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

    private CompletableFuture<PagoResponse> consultarEstadoPagoFallback(String paymentId, Throwable error) {
        return estadoPagoFallido("la transacción " + paymentId, error);
    }

    private CompletableFuture<PagoResponse> consultarEstadoPagoPorOrdenFallback(String ordenId, Throwable error) {
        return estadoPagoFallido("la orden " + ordenId, error);
    }

    private CompletableFuture<PagoResponse> estadoPagoFallido(String referencia, Throwable error) {
        Throwable causa = Futures.unwrap(error);
        log.error("No se pudo consultar el estado de pago para {}: {}", referencia, causa.toString());
        return CompletableFuture.failedFuture(new NegocioException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo consultar el estado real del pago; la orden permanece pendiente"));
    }
}
