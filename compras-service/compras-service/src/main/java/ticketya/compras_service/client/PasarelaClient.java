package ticketya.compras_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import ticketya.compras_service.exception.NegocioException;

@Component
public class PasarelaClient {

    private final RestClient restClient;

    public PasarelaClient(@Value("${pasarela.service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public PagoResponse procesarPago(PagoRequest request) {
        try {
            return restClient.post()
                    .uri("/api/pagos/procesar")
                    .body(request)
                    .retrieve()
                    .body(PagoResponse.class);
        } catch (HttpServerErrorException e) {
            throw new NegocioException(HttpStatus.BAD_GATEWAY,
                    "La pasarela de pagos devolvió un error de servidor (5xx): " + e.getStatusCode());
        } catch (HttpClientErrorException e) {
            throw new NegocioException(HttpStatus.valueOf(e.getStatusCode().value()),
                    "Error al comunicarse con la pasarela de pagos: " + e.getMessage());
        } catch (ResourceAccessException e) {
            throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE, "pasarela-mock-service no responde");
        }
    }
}
