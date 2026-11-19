package com.ticketya.entradas.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

import com.ticketya.entradas.filter.CorrelationIdExchangeFilter;

/**
 * Clientes HTTP no bloqueantes para las dependencias remotas de entradas-service.
 * Los timeouts de conexión y de respuesta se fijan en 2 s según el ADR 0006.
 */
@Configuration
public class WebClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(2);

    @Bean
    public WebClient comprasWebClient(@Value("${app.services.compras.url}") String baseUrl) {
        return build(baseUrl);
    }

    @Bean
    public WebClient eventosWebClient(@Value("${app.services.eventos.url}") String baseUrl) {
        return build(baseUrl);
    }

    @Bean
    public WebClient pasarelaWebClient(@Value("${app.services.pasarela.url}") String baseUrl) {
        return build(baseUrl);
    }

    private WebClient build(String baseUrl) {
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
