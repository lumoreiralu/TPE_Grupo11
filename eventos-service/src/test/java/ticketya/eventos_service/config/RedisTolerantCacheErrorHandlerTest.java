package ticketya.eventos_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.data.redis.RedisConnectionFailureException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisTolerantCacheErrorHandlerTest {

    private final RedisTolerantCacheErrorHandler handler = new RedisTolerantCacheErrorHandler();
    private final Cache cache = mock(Cache.class);
    private final RuntimeException error = new RedisConnectionFailureException("Redis caído");

    @Test
    void noPropagaFallasDeRedis() {
        when(cache.getName()).thenReturn(CacheConfig.CATALOGO);

        assertDoesNotThrow(() -> handler.handleCacheGetError(error, cache, "clave"));
        assertDoesNotThrow(() -> handler.handleCachePutError(error, cache, "clave", "valor"));
        assertDoesNotThrow(() -> handler.handleCacheEvictError(error, cache, "clave"));
        assertDoesNotThrow(() -> handler.handleCacheClearError(error, cache));
    }
}
