package ticketya.eventos_service.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import ticketya.eventos_service.filter.CorrelationIdExchangeFilter;

import java.time.Duration;

/**
 * Cliente HTTP no bloqueante para entradas-service. Los timeouts de conexión y
 * de respuesta se fijan en 2 s según el ADR 0006.
 */
@Configuration
public class WebClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(2);

    @Bean
    public WebClient entradasWebClient(@Value("${app.services.entradas.url}") String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis())
                .responseTimeout(RESPONSE_TIMEOUT);

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(CorrelationIdExchangeFilter.create())
                .build();
    }
}
