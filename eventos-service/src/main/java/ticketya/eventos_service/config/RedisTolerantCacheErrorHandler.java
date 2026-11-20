package ticketya.eventos_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.stereotype.Component;

@Component
public class RedisTolerantCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(RedisTolerantCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        avisar("GET", cache, key, exception);
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        avisar("PUT", cache, key, exception);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        avisar("EVICT", cache, key, exception);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        avisar("CLEAR", cache, "-", exception);
    }

    private void avisar(String operacion, Cache cache, Object key, RuntimeException exception) {
        log.warn("Cache backend unavailable; falling back to MongoDB. cache={} operation={} key={} "
                        + "correlationId={} error={}",
                cache != null ? cache.getName() : "-", operacion, key,
                MDC.get("correlationId"), exception.getMessage());
    }
}
