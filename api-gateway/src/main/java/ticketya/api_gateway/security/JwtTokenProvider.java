package ticketya.api_gateway.security;


import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;


@Component 
public class JwtTokenProvider {
    private final SecretKey key;
    private final long expirationInMs;

    public JwtTokenProvider(
        @Value("${jwt.secret:ClaveSecretaSuperSeguraParaTicketYa2026MasterKey}") String secret,
        @Value("${jwt.expiration:3600000}") long expirationInMs) {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            this.expirationInMs = expirationInMs;
    }

    //genera el token con usuario y su rol
    public String generarToken(String username, List<String> roles) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expirationInMs);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(key)
                .compact();
    }

    //valida la firma y el tiempo de expiración del token
    public boolean validarToken(String token) {
        try {
            Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    //extrae el usuario (subject)
    public String obtenerUserName(String token){
        return obtenerClaims(token).getSubject();
    }

    //extrae los roles del token
    @SuppressWarnings ("unchecked")
    public List<String> obtenerRoles(String token) {
        return obtenerClaims(token).get("roles", List.class);
    }

    private Claims obtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }




}
