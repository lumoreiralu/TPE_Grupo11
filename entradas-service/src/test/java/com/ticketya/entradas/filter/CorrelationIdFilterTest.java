package com.ticketya.entradas.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void peticionSinId_generaUnoNuevo_yLimpiaMDC() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcCorrelationIdDuringExecution = new AtomicReference<>();
        AtomicReference<String> mdcServiceDuringExecution = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            mdcCorrelationIdDuringExecution.set(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY));
            mdcServiceDuringExecution.set(MDC.get(CorrelationIdFilter.MDC_SERVICE_KEY));
        };

        filter.doFilter(request, response, chain);

        String generatedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedId).isNotNull().isNotBlank();
        assertThat(mdcCorrelationIdDuringExecution.get()).isEqualTo(generatedId);
        assertThat(mdcServiceDuringExecution.get()).isEqualTo("entradas-service");

        // MDC must be cleared in finally
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
        assertThat(MDC.get(CorrelationIdFilter.MDC_SERVICE_KEY)).isNull();
    }

    @Test
    void peticionConId_loConserva() throws ServletException, IOException {
        String existingId = "corr-test-456";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcCorrelationIdDuringExecution = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            mdcCorrelationIdDuringExecution.set(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY));
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(existingId);
        assertThat(mdcCorrelationIdDuringExecution.get()).isEqualTo(existingId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
    }

    @Test
    void configuracionDeLogsIncluyeServicioYCorrelationId() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("logback-spring.xml")) {
            assertThat(in).isNotNull();
            String config = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(config)
                    .contains("LogstashEncoder")
                    .contains("\"service\":\"entradas-service\"")
                    .contains("<includeMdcKeyName>correlationId</includeMdcKeyName>");
        }
    }
}
