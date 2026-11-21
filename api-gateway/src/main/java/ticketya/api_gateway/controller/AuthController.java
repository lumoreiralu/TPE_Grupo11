package ticketya.api_gateway.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import ticketya.api_gateway.dto.AuthResponse;
import ticketya.api_gateway.dto.LoginRequest;
import ticketya.api_gateway.model.Usuario;
import ticketya.api_gateway.repository.UsuarioRepository;
import ticketya.api_gateway.security.JwtTokenProvider;

@RestController 
@RequestMapping ("/api/auth")
public class AuthController {
    private final UsuarioRepository usuarioRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(UsuarioRepository usuarioRepository, JwtTokenProvider jwtTokenProvider) {
        this.usuarioRepository = usuarioRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }


    @PostMapping("/register")
    public Mono<ResponseEntity<String>> register(@RequestBody Usuario usuario) {

        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            return Mono.just(
                ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("El nombre de usuario ya está en uso")
            );
        }

        if(usuario.getUsername() == null || usuario.getPassword() == null || usuario.getEmail() == null) {
            return Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Faltan datos requeridos")
            );
        }
        if(usuario.getUsername().isEmpty() || usuario.getPassword().isEmpty() || usuario.getEmail().isEmpty()) {
            return Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Los campos no pueden estar vacíos")
            );
        }
        if( !usuario.getEmail().contains("@")) {
            return Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El email no es válido")
            );
        }
        // Se registra automáticamente con el rol CLIENTE
        usuarioRepository.registrarUsuario(
            usuario.getUsername(),
            usuario.getPassword(),
            usuario.getEmail()
        );

        return Mono.just(
            ResponseEntity.status(HttpStatus.CREATED)
                .body("Usuario registrado exitosamente")
        );
    }


    @PostMapping ("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest loginRequest) {
        Optional<Usuario> usuarioOptional = usuarioRepository.findByUsername(loginRequest.getUsername());
        if(usuarioOptional.isPresent()) {
            Usuario usuario = usuarioOptional.get();
            //verifica la clave enviada contra el hash BCypt guardado
            if(usuarioRepository.getPasswordEncoder().matches(loginRequest.getPassword(), usuario.getPassword())) {
                List<String> roles = List.of(usuario.getRol().name());
                String token = jwtTokenProvider.generarToken(usuario.getUsername(), roles);
                return Mono.just(ResponseEntity.ok(new AuthResponse(token, usuario.getUsername(), roles)));
            }
        }
        //si el usuario no existe o la contraseña no coincide 
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
