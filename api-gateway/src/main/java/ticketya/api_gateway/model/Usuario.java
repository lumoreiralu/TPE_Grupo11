package ticketya.api_gateway.model;

import java.util.UUID;

public class Usuario {
    private UUID id; 
    private String username; 
    private String password; // Hash BCrypt 
    private String email; 
    private Rol rol;

    public Usuario() {
    }

    public Usuario(UUID id, String username, String password, String email, Rol rol) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.rol = rol;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    
}
