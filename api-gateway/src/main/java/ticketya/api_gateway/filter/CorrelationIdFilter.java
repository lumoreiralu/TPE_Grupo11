package ticketya.api_gateway.filter;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements WebFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";
    public static final String MDC_SERVICE_KEY = "service";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String finalCorrelationId = correlationId;

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.header(CORRELATION_ID_HEADER, finalCorrelationId))
                .build();
        mutatedExchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, finalCorrelationId);

        // MDC se completa antes de ejecutar la cadena para que quede disponible en los
        // logs que emite el servicio durante el procesamiento de la petición.
        MDC.put(MDC_CORRELATION_ID_KEY, finalCorrelationId);
        MDC.put(MDC_SERVICE_KEY, "api-gateway");

        return chain.filter(mutatedExchange)
                .contextWrite(context -> context.put(MDC_CORRELATION_ID_KEY, finalCorrelationId))
                .doFinally(signalType -> {
                    MDC.remove(MDC_CORRELATION_ID_KEY);
                    MDC.remove(MDC_SERVICE_KEY);
                });
    }
}
