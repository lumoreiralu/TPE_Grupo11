package ticketya.api_gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                //rutas publicas (login, openapi/swagger, health)
                .pathMatchers("/api/auth/login", "/v3/api-docs/**", "/swagger-ui/**", "/webjars/**", "/actuator/**", "/fallback/**").permitAll()
                .pathMatchers("/api/eventos/publicados/**").permitAll()

                //matriz de accesos por rol
                .pathMatchers("/api/lugares/**").hasAuthority("ROLE_ORGANIZADOR")
                .pathMatchers("/api/entradas/validar-qr").hasAuthority("ROLE_PERSONAL_ACCESO")
                .pathMatchers("/api/entradas/devoluciones/*/aprobar").hasAnyAuthority("ROLE_ADMINISTRADOR")
                .pathMatchers("/api/entradas/devoluciones/*/rechazar").hasAnyAuthority("ROLE_ADMINISTRADOR")
                .pathMatchers("/api/entradas/metricas/**").hasAnyAuthority("ROLE_ADMINISTRADOR", "ROLE_ORGANIZADOR")
                .pathMatchers("/api/compras/metricas/**").hasAnyAuthority("ROLE_ADMINISTRADOR", "ROLE_ORGANIZADOR")
                .pathMatchers("/api/compras/*/compensar").hasAuthority("ROLE_ADMINISTRADOR")
                .pathMatchers("/api/fila/**").hasAnyAuthority("ROLE_CLIENTE")

                //rutas de cliente y compra
                .pathMatchers("/api/compras/**", "/api/entradas/cliente/**").hasAnyAuthority("ROLE_CLIENTE", "ROLE_ADMINISTRADOR")

                //cualquier otra ruta requiere token valido
                .anyExchange().authenticated()
    )
                //agrega el filtro de autenticacion JWT
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }


}
