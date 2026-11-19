package ticketya.api_gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(
                "ClaveSecretaSuperSeguraParaTicketYa2026MasterKey",
                3600000L
        );
    }

    @Test
    void loginCorrecto_generaTokenValido() {
        String token = provider.generarToken("cliente1", List.of("ROLE_CLIENTE"));
        assertThat(token).isNotNull();
        assertThat(provider.validarToken(token)).isTrue();
        assertThat(provider.obtenerUserName(token)).isEqualTo("cliente1");
        assertThat(provider.obtenerRoles(token)).containsExactly("ROLE_CLIENTE");
    }

    @Test
    void tokenExpirado_esInvalido() throws InterruptedException {
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(
                "ClaveSecretaSuperSeguraParaTicketYa2026MasterKey",
                1L // 1 ms
        );
        String token = shortLivedProvider.generarToken("cliente1", List.of("ROLE_CLIENTE"));
        Thread.sleep(15);
        assertThat(shortLivedProvider.validarToken(token)).isFalse();
    }

    @Test
    void firmaInvalida_esRechazada() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "OtraClaveTotalmenteDiferente12345678901234567890",
                3600000L
        );
        String token = otherProvider.generarToken("cliente1", List.of("ROLE_CLIENTE"));
        assertThat(provider.validarToken(token)).isFalse();
    }
}
