package ticketya.eventos_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ticketya.eventos_service.config.CacheConfig;
import ticketya.eventos_service.config.RedisTolerantCacheErrorHandler;
import ticketya.eventos_service.model.EstadoEvento;
import ticketya.eventos_service.model.Evento;
import ticketya.eventos_service.model.TipoEvento;
import ticketya.eventos_service.repository.EventoRepository;
import ticketya.eventos_service.repository.LugarRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CatalogoCacheServiceTest.TestConfig.class)
class CatalogoCacheServiceTest {

    @Autowired
    private CatalogoCacheService catalogoCacheService;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private LugarRepository lugarRepository;

    @Autowired
    private EventoBusquedaService busquedaService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void limpiarCache() {
        cacheManager.getCache(CacheConfig.CATALOGO).clear();
        reset(eventoRepository, lugarRepository, busquedaService);
    }

    @Test
    void segundaConsultaNoAccedeAMongo() {
        Evento evento = evento("evt-1");
        when(eventoRepository.findByEstado(EstadoEvento.PUBLICADO)).thenReturn(List.of(evento));
        when(eventoRepository.findById("evt-1")).thenReturn(java.util.Optional.of(evento));
        when(busquedaService.buscar(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(evento));

        assertEquals(List.of(evento), catalogoCacheService.obtenerPublicados());
        assertEquals(List.of(evento), catalogoCacheService.obtenerPublicados());
        assertEquals(java.util.Optional.of(evento), catalogoCacheService.obtenerPorId("evt-1"));
        assertEquals(java.util.Optional.of(evento), catalogoCacheService.obtenerPorId("evt-1"));
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        assertEquals(List.of(evento), catalogoCacheService.buscar(
                TipoEvento.RECITAL, "org-1", "Artista", "Buenos Aires", desde, hasta, "fecha"));
        assertEquals(List.of(evento), catalogoCacheService.buscar(
                TipoEvento.RECITAL, "org-1", "Artista", "Buenos Aires", desde, hasta, "fecha"));

        verify(eventoRepository, times(1)).findByEstado(EstadoEvento.PUBLICADO);
        verify(eventoRepository, times(1)).findById("evt-1");
        verify(busquedaService, times(1)).buscar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void invalidarCatalogoObligaAConsultarMongoNuevamente() {
        Evento evento = evento("evt-2");
        when(eventoRepository.findByEstado(EstadoEvento.PUBLICADO)).thenReturn(List.of(evento));

        catalogoCacheService.obtenerPublicados();
        catalogoCacheService.invalidarCatalogo();
        catalogoCacheService.obtenerPublicados();

        verify(eventoRepository, times(2)).findByEstado(EstadoEvento.PUBLICADO);
    }

    private Evento evento(String id) {
        Evento evento = new Evento();
        evento.setId(id);
        evento.setNombre("Recital");
        evento.setEstado(EstadoEvento.PUBLICADO);
        return evento;
    }

    @Configuration
    @EnableCaching
    static class TestConfig implements CachingConfigurer {

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheConfig.CATALOGO);
        }

        @Bean
        EventoRepository eventoRepository() {
            return mock(EventoRepository.class);
        }

        @Bean
        LugarRepository lugarRepository() {
            return mock(LugarRepository.class);
        }

        @Bean
        EventoBusquedaService busquedaService() {
            return mock(EventoBusquedaService.class);
        }

        @Bean
        CatalogoCacheService catalogoCacheService() {
            return new CatalogoCacheService();
        }

        @Override
        public CacheErrorHandler errorHandler() {
            return new RedisTolerantCacheErrorHandler();
        }
    }
}
