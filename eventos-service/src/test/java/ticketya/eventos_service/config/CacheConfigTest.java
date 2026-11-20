package ticketya.eventos_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import ticketya.eventos_service.model.Evento;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    void catalogoCacheTieneTtlDe60Segundos() {
        CacheManager cacheManager = cacheConfig.cacheManager(mock(RedisConnectionFactory.class));

        RedisCache cache = assertInstanceOf(RedisCache.class, cacheManager.getCache(CacheConfig.CATALOGO));

        assertEquals(Duration.ofSeconds(60),
                cache.getCacheConfiguration().getTtlFunction().getTimeToLive(CacheConfig.CATALOGO, "clave"));
    }

    @Test
    void catalogoExpiraEn60Segundos() {
        RedisCacheWriter writer = mock(RedisCacheWriter.class);
        CacheManager cacheManager = RedisCacheManager.builder(writer)
                .cacheDefaults(cacheConfig.configuracionCatalogo())
                .withCacheConfiguration(CacheConfig.CATALOGO, cacheConfig.configuracionCatalogo())
                .build();

        cacheManager.getCache(CacheConfig.CATALOGO).put("clave", "valor");

        verify(writer).put(eq(CacheConfig.CATALOGO), any(byte[].class), any(byte[].class),
                eq(Duration.ofSeconds(60)));
    }

    @Test
    void cuerpoDelCatalogoEsSerializable() throws Exception {
        Evento evento = new Evento();
        evento.setId("evt-1");
        evento.setNombre("Recital");
        List<Evento> cuerpo = List.of(evento);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream salida = new ObjectOutputStream(bytes)) {
            salida.writeObject(cuerpo);
        }

        try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            @SuppressWarnings("unchecked")
            List<Evento> restaurado = (List<Evento>) entrada.readObject();
            assertEquals("Recital", restaurado.get(0).getNombre());
        }
    }
}
