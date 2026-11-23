package ticketya.api_gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Rutas públicas (login, OpenAPI/Swagger, health, catálogo público y consulta de lugares)
                        .pathMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/actuator/**",
                                "/fallback/**"
                        ).permitAll()
                        .pathMatchers(HttpMethod.GET,
                                "/api/eventos",
                                "/api/eventos/publicados/**",
                                "/api/eventos/buscar",
                                "/api/eventos/*",
                                "/api/lugares/**"
                        ).permitAll()

                        // Gestión de eventos (crear, publicar, cancelar): Organizador y Administrador
                        .pathMatchers(HttpMethod.POST, "/api/eventos", "/api/eventos/*/cancelar")
                                .hasAnyAuthority("ROLE_ORGANIZADOR", "ROLE_ADMINISTRADOR")
                        .pathMatchers(HttpMethod.PUT, "/api/eventos/*/publicar")
                                .hasAnyAuthority("ROLE_ORGANIZADOR", "ROLE_ADMINISTRADOR")

                        // Gestión de recintos/lugares (alta de lugares y sectores)
                        .pathMatchers("/api/lugares/**")
                                .hasAnyAuthority("ROLE_ORGANIZADOR", "ROLE_ADMINISTRADOR")

                        // Personal de acceso: validación de QR
                        .pathMatchers("/api/entradas/validar-qr")
                                .hasAuthority("ROLE_PERSONAL_ACCESO")

                        // Cliente: preferencias, recomendaciones, fila, compras, devoluciones y consulta de sus entradas
                        .pathMatchers("/api/eventos/preferencias", "/api/eventos/recomendados", "/api/fila/**")
                                .hasAuthority("ROLE_CLIENTE")
                        .pathMatchers(HttpMethod.POST, "/api/entradas/*/devolucion")
                                .hasAuthority("ROLE_CLIENTE")
                        .pathMatchers("/api/compras/**", "/api/entradas/cliente/**")
                                .hasAnyAuthority("ROLE_CLIENTE", "ROLE_ADMINISTRADOR")

                        // Devoluciones (aprobar / rechazar) y compensación manual: Administrador
                        .pathMatchers("/api/entradas/devoluciones/*/aprobar", "/api/entradas/devoluciones/*/rechazar")
                                .hasAuthority("ROLE_ADMINISTRADOR")
                        .pathMatchers("/api/compras/*/compensar")
                                .hasAuthority("ROLE_ADMINISTRADOR")

                        // Métricas y reportes: Organizador y Administrador
                        .pathMatchers("/api/entradas/metricas/**", "/api/compras/metricas/**")
                                .hasAnyAuthority("ROLE_ORGANIZADOR", "ROLE_ADMINISTRADOR")

                        // Cualquier otra ruta requiere token válido
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}