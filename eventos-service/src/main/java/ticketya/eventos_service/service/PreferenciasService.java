package ticketya.eventos_service.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ticketya.eventos_service.dto.PreferenciasRequest;
import ticketya.eventos_service.model.PreferenciasCliente;
import ticketya.eventos_service.model.TipoEvento;
import ticketya.eventos_service.repository.PreferenciasClienteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PreferenciasService {

    private static final int MAX_ARTISTAS = 20;
    private static final int MAX_LONGITUD_ARTISTA = 120;
    private static final int MAX_LONGITUD_CIUDAD = 120;

    private final PreferenciasClienteRepository repository;

    public PreferenciasService(PreferenciasClienteRepository repository) {
        this.repository = repository;
    }

    public PreferenciasCliente guardar(String clienteId, PreferenciasRequest request) {
        PreferenciasCliente preferencias = new PreferenciasCliente();
        preferencias.setClienteId(validarClienteId(clienteId));
        preferencias.setTiposEvento(normalizarTipos(request == null ? null : request.tiposEvento()));
        preferencias.setArtistasFavoritos(normalizarArtistas(request == null ? null : request.artistasFavoritos()));
        preferencias.setCiudad(normalizarCiudad(request == null ? null : request.ciudad()));
        preferencias.setFechaActualizacion(LocalDateTime.now());
        return repository.save(preferencias);
    }

    public PreferenciasCliente obtener(String clienteId) {
        return repository.findById(validarClienteId(clienteId)).orElseGet(() -> {
            PreferenciasCliente vacias = new PreferenciasCliente();
            vacias.setClienteId(clienteId.trim());
            vacias.setTiposEvento(new LinkedHashSet<>());
            vacias.setArtistasFavoritos(new ArrayList<>());
            return vacias;
        });
    }

    public void eliminar(String clienteId) {
        repository.deleteById(validarClienteId(clienteId));
    }

    private String validarClienteId(String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la identidad del cliente");
        }
        return clienteId.trim();
    }

    private Set<TipoEvento> normalizarTipos(Set<TipoEvento> tipos) {
        if (tipos == null) {
            return new LinkedHashSet<>();
        }
        Set<TipoEvento> normalizados = new LinkedHashSet<>();
        tipos.stream()
                .filter(tipo -> tipo != null)
                .forEach(normalizados::add);
        return normalizados;
    }

    private List<String> normalizarArtistas(List<String> artistas) {
        if (artistas == null || artistas.isEmpty()) {
            return new ArrayList<>();
        }
        if (artistas.size() > MAX_ARTISTAS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Se pueden registrar hasta " + MAX_ARTISTAS + " artistas favoritos");
        }

        Set<String> vistos = new HashSet<>();
        List<String> resultado = new ArrayList<>();
        for (String artista : artistas) {
            if (artista == null) {
                continue;
            }
            String normalizado = artista.trim();
            if (normalizado.length() > MAX_LONGITUD_ARTISTA) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El nombre de cada artista no puede superar " + MAX_LONGITUD_ARTISTA + " caracteres");
            }
            if (!normalizado.isBlank() && vistos.add(normalizado.toLowerCase(Locale.ROOT))) {
                resultado.add(normalizado);
            }
        }
        return resultado;
    }

    private String normalizarCiudad(String ciudad) {
        if (ciudad == null || ciudad.isBlank()) {
            return null;
        }
        String normalizada = ciudad.trim();
        if (normalizada.length() > MAX_LONGITUD_CIUDAD) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La ciudad no puede superar " + MAX_LONGITUD_CIUDAD + " caracteres");
        }
        return normalizada;
    }
}
