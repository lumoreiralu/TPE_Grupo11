package com.ticketya.entradas.client;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.util.Map;

@Component
public class PasarelaClient {
    private final RestClient restClient;

    public PasarelaClient() {
        this.restClient = RestClient.builder().baseUrl("http://localhost:8084").build();
    }

    public void solicitarReembolso(String ordenCompraId, String clienteId) {
        try {
            restClient.post() .uri("/api/pagos/reembolsar")
                    .body(Map.of("ordenId", ordenCompraId, "clienteId", clienteId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.err.println("Aviso: Reembolso simulado registrado para orden " + ordenCompraId);
        }
    }
}
