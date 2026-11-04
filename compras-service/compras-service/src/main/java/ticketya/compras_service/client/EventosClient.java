package ticketya.compras_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import ticketya.compras_service.exception.NegocioException;

@Component
public class EventosClient {

    private final RestClient restClient;

    public EventosClient(@Value("${eventos.service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public EventoDTO obtenerEvento(String id) {
        try {
            return restClient.get()
                    .uri("/api/eventos/{id}", id)
                    .retrieve()
                    .body(EventoDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new NegocioException(HttpStatus.NOT_FOUND, "El evento " + id + " no existe");
        } catch (ResourceAccessException e) {
            throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE, "eventos-service no responde");
        }
    }
}