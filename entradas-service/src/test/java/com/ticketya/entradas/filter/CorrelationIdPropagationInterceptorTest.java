package com.ticketya.entradas.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdPropagationInterceptorTest {

    private final CorrelationIdPropagationInterceptor interceptor = new CorrelationIdPropagationInterceptor();

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void propagaElCorrelationIdDelMdcALlamadaSaliente() throws Exception {
        MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, "corr-saliente-42");
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("/api/compras/asientos/liberar"));

        interceptor.intercept(request, new byte[0],
                (req, body) -> new MockClientHttpResponse(new byte[0], HttpStatus.OK));

        assertThat(request.getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isEqualTo("corr-saliente-42");
    }

    @Test
    void sinCorrelationIdEnMdc_noAgregaHeader() throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("/api/eventos/1"));

        interceptor.intercept(request, new byte[0],
                (req, body) -> new MockClientHttpResponse(new byte[0], HttpStatus.OK));

        assertThat(request.getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER)).isNull();
    }
}
