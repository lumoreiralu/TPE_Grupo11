package ticketya.eventos_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ticketya.eventos_service.config.CacheConfig;
import ticketya.eventos_service.config.RedisTolerantCacheErrorHandler;
import ticketya.eventos_service.model.EstadoEvento;
import ticketya.eventos_service.model.Evento;
import ticketya.eventos_service.repository.EventoRepository;
import ticketya.eventos_service.repository.LugarRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CatalogoCacheDegradationTest.TestConfig.class)
class CatalogoCacheDegradationTest {

    @Autowired
    private CatalogoCacheService catalogoCacheService;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void simularRedisCaido() {
        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn(CacheConfig.CATALOGO);
        when(cache.get(any())).thenThrow(new RedisConnectionFailureException("Redis caído"));
        when(cacheManager.getCache(CacheConfig.CATALOGO)).thenReturn(cache);
    }

    @Test
    void fallaDeRedisConsultaMongoIgualmente() {
        Evento evento = new Evento();
        evento.setId("evt-1");
        evento.setNombre("Recital");
        evento.setEstado(EstadoEvento.PUBLICADO);
        when(eventoRepository.findByEstado(EstadoEvento.PUBLICADO)).thenReturn(List.of(evento));

        List<Evento> resultado = assertDoesNotThrow(() -> catalogoCacheService.obtenerPublicados());

        assertEquals(List.of(evento), resultado);
        verify(eventoRepository).findByEstado(EstadoEvento.PUBLICADO);
    }

    @Configuration
    @EnableCaching
    static class TestConfig implements CachingConfigurer {

        @Bean
        public CacheManager cacheManager() {
            return mock(CacheManager.class);
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
