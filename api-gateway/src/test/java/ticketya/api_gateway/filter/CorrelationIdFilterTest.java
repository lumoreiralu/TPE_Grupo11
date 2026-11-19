package ticketya.api_gateway.filter;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void peticionSinId_generaUnoNuevo_yLimpiaMDC() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> downstreamHeader = new AtomicReference<>();
        AtomicReference<String> mdcCorrelationId = new AtomicReference<>();
        AtomicReference<String> mdcService = new AtomicReference<>();

        WebFilterChain chain = mutatedExchange -> {
            downstreamHeader.set(mutatedExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
            mdcCorrelationId.set(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY));
            mdcService.set(MDC.get(CorrelationIdFilter.MDC_SERVICE_KEY));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        String responseHeader = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(responseHeader).isNotNull().isNotBlank();
        assertThat(downstreamHeader.get()).isEqualTo(responseHeader);
        assertThat(mdcCorrelationId.get()).isEqualTo(responseHeader);
        assertThat(mdcService.get()).isEqualTo("api-gateway");

        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
        assertThat(MDC.get(CorrelationIdFilter.MDC_SERVICE_KEY)).isNull();
    }

    @Test
    void peticionConId_loConserva() {
        String existingId = "custom-corr-id-999";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> downstreamHeader = new AtomicReference<>();
        WebFilterChain chain = mutatedExchange -> {
            downstreamHeader.set(mutatedExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        String responseHeader = exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(responseHeader).isEqualTo(existingId);
        assertThat(downstreamHeader.get()).isEqualTo(existingId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY)).isNull();
    }

    @Test
    void configuracionDeLogsIncluyeServicioYCorrelationId() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("logback-spring.xml")) {
            assertThat(in).isNotNull();
            String config = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(config)
                    .contains("LogstashEncoder")
                    .contains("\"service\":\"api-gateway\"")
                    .contains("<includeMdcKeyName>correlationId</includeMdcKeyName>");
        }
    }
}
