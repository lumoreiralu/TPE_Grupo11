package com.ticketya.entradas.filter;

import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

/**
 * Equivalente reactivo de {@link CorrelationIdPropagationInterceptor}: copia el
 * correlationId del MDC a las llamadas salientes hechas con WebClient.
 */
public final class CorrelationIdExchangeFilter {

    private CorrelationIdExchangeFilter() {
    }

    public static ExchangeFilterFunction create() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
            if (correlationId == null || correlationId.isBlank()) {
                return Mono.just(request);
            }

            ClientRequest requestConCorrelacion = ClientRequest.from(request)
                    .header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId)
                    .build();
            return Mono.just(requestConCorrelacion);
        });
    }
}
