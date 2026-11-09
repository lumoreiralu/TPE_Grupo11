package ticketya.compras_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import ticketya.compras_service.exception.NegocioException;

import java.util.List;

@Component
public class EntradasClient {

    private final RestClient restClient;

    public EntradasClient(@Value("${entradas.service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public EmisionResponse emitirEntradas(EmisionRequest request) {
        try {
            return restClient.post()
                    .uri("/api/entradas/emitir")
                    .body(request)
                    .retrieve()
                    .body(EmisionResponse.class);
        } catch (HttpServerErrorException e) {
            throw new NegocioException(HttpStatus.BAD_GATEWAY,
                    "El servicio de entradas devolvió un error de servidor (5xx): " + e.getStatusCode());
        } catch (HttpClientErrorException e) {
            throw new NegocioException(HttpStatus.valueOf(e.getStatusCode().value()),
                    "Error al comunicarse con entradas-service: " + e.getMessage());
        } catch (ResourceAccessException e) {
            throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE, "entradas-service no responde");
        }
    }

    public List<EmisionResponse.EntradaDTO> obtenerEntradasPorOrden(String ordenCompraId) {
        try {
            return restClient.get()
                    .uri("/api/entradas/orden/{ordenCompraId}", ordenCompraId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<EmisionResponse.EntradaDTO>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
