package com.ticketya.entradas.client;


import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ComprasClient {
    private final RestClient restClient;

    public ComprasClient(){
        this.restClient = RestClient.builder().baseUrl("http://localhost:8082").build();
    }

    public void liberarAsiento(String eventoId, String sectorId, String fila, Integer asiento){
        restClient.post()
                .uri("/api/compras/asientos/liberar")
                .body(new LiberarAsientoRequest(eventoId, sectorId, fila, asiento))
                .retrieve()
                .toBodilessEntity();
    }

    public record LiberarAsientoRequest(String eventoId, String sectorId, String fila, Integer asiento){}
}
