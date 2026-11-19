package com.ticketya.entradas.filter;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Copia el correlationId del MDC a las llamadas HTTP salientes para que el
 * servicio destino registre la misma traza que la petición original.
 */
public class CorrelationIdPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        if (correlationId != null && !correlationId.isBlank()) {
            request.getHeaders().set(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
        }
        return execution.execute(request, body);
    }
}
