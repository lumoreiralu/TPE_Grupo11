package ticketya.api_gateway.security;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component  
public class JwtAuthenticationFilter implements WebFilter {
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override 
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) { 
            String token = bearerToken.substring(7); 
            if (jwtTokenProvider.validarToken(token)) { 
                String username = jwtTokenProvider.obtenerUserName(token); 
                List<String> roles = jwtTokenProvider.obtenerRoles(token);
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
                //mutate request to add X-User-Id and X-User-Roles headers for downstream services
                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r.header("X-User-Id", username)
                                       .header("X-User-Roles", String.join(",", roles)))
                        .build();
                //carga la autenticacion en el contexto reactivo de spring security
                return chain.filter(mutatedExchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
            }
        }
        //si no hay token o es invalido, continua con la cadena de filtros sin autenticacion
        return chain.filter(exchange);

    }
}
