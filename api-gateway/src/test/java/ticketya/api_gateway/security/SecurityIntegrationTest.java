package ticketya.api_gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import ticketya.api_gateway.dto.LoginRequest;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private WebTestClient webTestClient;

    @BeforeEach
    void setup() {
        this.webTestClient = WebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    void loginCorrecto_devuelveTokenY200() {
        LoginRequest request = new LoginRequest("cliente1", "cliente123");

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.username").isEqualTo("cliente1")
                .jsonPath("$.roles[0]").isEqualTo("ROLE_CLIENTE");
    }

    @Test
    void loginInvalido_devuelve401() {
        LoginRequest request = new LoginRequest("cliente1", "wrongpassword");

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rutaProtegidaSinToken_devuelve401() {
        webTestClient.post()
                .uri("/api/entradas/validar-qr")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void consultarLugarSinToken_noEsRechazadoPorAutorizacion() {
        webTestClient.get()
                .uri("/api/lugares/lugar-teatro")
                .exchange()
                .expectStatus()
                .value(status -> org.assertj.core.api.Assertions.assertThat(status)
                        .isNotEqualTo(401)
                        .isNotEqualTo(403));
    }

    @Test
    void rolInsuficiente_devuelve403() {
        // cliente1 intenta acceder a ruta de ROLE_PERSONAL_ACCESO (/api/entradas/validar-qr)
        String tokenCliente = jwtTokenProvider.generarToken("cliente1", List.of("ROLE_CLIENTE"));

        webTestClient.post()
                .uri("/api/entradas/validar-qr")
                .header("Authorization", "Bearer " + tokenCliente)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void actuatorHealth_respondeStatusOk() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void rolInsuficiente_noPuedeConsultarRecomendaciones() {
        String tokenOrganizador = jwtTokenProvider.generarToken(
                "organizador1", List.of("ROLE_ORGANIZADOR"));

        webTestClient.get()
                .uri("/api/eventos/recomendados")
                .header("Authorization", "Bearer " + tokenOrganizador)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void rolSuficienteAdmin_noDevuelve403ParaCompensar() {
        // admin tiene ROLE_ADMINISTRADOR para /api/compras/*/compensar
        String tokenAdmin = jwtTokenProvider.generarToken("admin", List.of("ROLE_ADMINISTRADOR"));

        webTestClient.post()
                .uri("/api/compras/123/compensar")
                .header("Authorization", "Bearer " + tokenAdmin)
                .exchange()
                .expectStatus().value(status -> org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(403).isNotEqualTo(401));
    }
}
