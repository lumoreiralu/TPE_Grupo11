package com.ticketya.entradas.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.time.LocalDateTime;

@Component
public class EventosClient {
    private final RestClient restClient;

    public EventosClient(){
        this.restClient = RestClient.builder().baseUrl("http://localhost:8081").build();
    }

    public record EventoFechaDTO(String id, LocalDateTime fechaHora){}

    public LocalDateTime obtenerFechaEvento(String eventoId){
        EventoFechaDTO evento = restClient.get()
                .uri("/api/eventos/{id}", eventoId)
                .retrieve()
                .body(EventoFechaDTO.class);
        return evento != null ? evento.fechaHora() : null;
    }
}
