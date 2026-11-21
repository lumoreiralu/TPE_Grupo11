package ticketya.api_gateway.repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import ticketya.api_gateway.model.Rol;
import ticketya.api_gateway.model.Usuario;

@Repository 
public class UsuarioRepository {
    private final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsuarioRepository(){
        //precarga de usuarios iniciales con hashing BCrypt
        cargarUsuario("admin", "admin123", "admin@ticketya.com", Rol.ROLE_ADMINISTRADOR); 
        cargarUsuario("organizador1", "org123", "org@ticketya.com", Rol.ROLE_ORGANIZADOR); 
        cargarUsuario("cliente1", "cliente123", "cliente@ticketya.com", Rol.ROLE_CLIENTE); 
        cargarUsuario("puerta1", "puerta123", "puerta@ticketya.com", Rol.ROLE_PERSONAL_ACCESO); 
    }

    private void cargarUsuario(String username, String password, String email, Rol rol) {
        String passwordHash = passwordEncoder.encode(password);
        Usuario usuario = new Usuario(UUID.randomUUID(), username, passwordHash, email, rol);
        usuarios.put(username.toLowerCase(), usuario);
    }

    public Optional<Usuario> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usuarios.get(username.toLowerCase()));
    }

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    public boolean existsByUsername(String username) {
        return usuarios.containsKey(username.toLowerCase());
    }

    public Usuario registrarUsuario(String username, String password, String email) {
        String passwordHash = passwordEncoder.encode(password);

        Usuario usuario = new Usuario(
            UUID.randomUUID(),
            username,
            passwordHash,
            email,
            Rol.ROLE_CLIENTE
        );

        usuarios.put(username.toLowerCase(), usuario);

        return usuario;
    }
}
