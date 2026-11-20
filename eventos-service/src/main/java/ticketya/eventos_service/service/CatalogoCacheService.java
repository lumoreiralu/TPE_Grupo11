package ticketya.eventos_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ticketya.eventos_service.config.CacheConfig;
import ticketya.eventos_service.model.EstadoEvento;
import ticketya.eventos_service.model.Evento;
import ticketya.eventos_service.model.TipoEvento;
import ticketya.eventos_service.repository.EventoRepository;
import ticketya.eventos_service.repository.LugarRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CatalogoCacheService {

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private LugarRepository lugarRepository;

    @Autowired
    private EventoBusquedaService busquedaService;

    @Cacheable(value = CacheConfig.CATALOGO)
    public List<Evento> obtenerPublicados() {
        List<Evento> publicados = eventoRepository.findByEstado(EstadoEvento.PUBLICADO);
        publicados.forEach(this::completarDatosLugar);
        return publicados;
    }

    @Cacheable(value = CacheConfig.CATALOGO)
    public Optional<Evento> obtenerPorId(String id) {
        return eventoRepository.findById(id)
                .map(evento -> {
                    completarDatosLugar(evento);
                    return evento;
                });
    }

    @Cacheable(value = CacheConfig.CATALOGO)
    public List<Evento> buscar(TipoEvento tipo, String organizadorId, String artista, String ciudad,
                               LocalDate fechaDesde, LocalDate fechaHasta, String ordenarPor) {
        List<Evento> resultado = busquedaService.buscar(
                tipo, organizadorId, artista, ciudad, fechaDesde, fechaHasta, ordenarPor);
        resultado.forEach(this::completarDatosLugar);
        return resultado;
    }

    @CacheEvict(value = CacheConfig.CATALOGO, allEntries = true)
    public void invalidarCatalogo() {
    }

    public void completarDatosLugar(Evento evento) {
        if (evento.getLugarId() != null) {
            lugarRepository.findById(evento.getLugarId()).ifPresent(lugar -> {
                if (evento.getCiudad() == null || evento.getCiudad().isBlank()) {
                    evento.setCiudad(lugar.getCiudad());
                }
                if (lugar.getSectores() != null) {
                    evento.setSectores(lugar.getSectores().stream()
                            .map(s -> new Evento.SectorResumen(
                                    s.getSectorId(),
                                    s.getNombre(),
                                    s.getTipo(),
                                    s.getCapacidadMaxima()))
                            .toList());
                }
            });
        }
    }
}
