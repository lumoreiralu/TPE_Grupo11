package ticketya.pasarela_mock.filter;

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
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/pagos/procesar");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcCorrelationId = new AtomicReference<>();
        AtomicReference<String> mdcService = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            mdcCorrelationId.set(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY));
            mdcService.set(MDC.get(CorrelationIdFilter.MDC_SERVICE_KEY));
        };

        filter.doFilter(request, response, chain);

        String generatedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedId).isNotNull().isNotBlank();
        assertThat(mdcCorrelationId.get()).isEqualTo(generatedId);
        assertThat(mdcService.get()).isEqualTo("pasarela-mock-service");

        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
        assertThat(MDC.get(CorrelationIdFilter.MDC_SERVICE_KEY)).isNull();
    }

    @Test
    void peticionConId_loConserva() throws ServletException, IOException {
        String existingId = "corr-pago-123";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/pagos/procesar");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcCorrelationId = new AtomicReference<>();
        FilterChain chain = (req, res) ->
                mdcCorrelationId.set(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY));

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(existingId);
        assertThat(mdcCorrelationId.get()).isEqualTo(existingId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
    }

    @Test
    void configuracionDeLogsIncluyeServicioYCorrelationId() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("logback-spring.xml")) {
            assertThat(in).isNotNull();
            String config = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(config)
                    .contains("LogstashEncoder")
                    .contains("\"service\":\"pasarela-mock-service\"")
                    .contains("<includeMdcKeyName>correlationId</includeMdcKeyName>");
        }
    }
}
