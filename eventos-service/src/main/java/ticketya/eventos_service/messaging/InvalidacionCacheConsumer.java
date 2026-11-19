package ticketya.eventos_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class InvalidacionCacheConsumer {
    private static final Logger log = LoggerFactory.getLogger(InvalidacionCacheConsumer.class);
    private final StringRedisTemplate redisTemplate;
    private final CacheManager cacheManager;

    public InvalidacionCacheConsumer(StringRedisTemplate redisTemplate,
                                     ObjectProvider<CacheManager> cacheManagerProvider) {
        this.redisTemplate = redisTemplate;
        this.cacheManager = cacheManagerProvider.getIfAvailable();
    }

    @RabbitListener(queues = "invalidacion-cache.q")
    public void handleInvalidacion(DomainEvent event,
                                   @Header(name = "X-Correlation-ID", required = false) String headerCorrelationId) {
        String correlationId = (headerCorrelationId != null && !headerCorrelationId.isBlank())
                ? headerCorrelationId
                : event.correlationId();
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        MDC.put("service", "eventos-service");
        try {
            String dedupeKey = "seen:invalidacion-cache.q:" + event.eventId();
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", Duration.ofHours(24));
            if (Boolean.FALSE.equals(isNew)) {
                log.info("Duplicate event ignored: eventId={}", event.eventId());
                return;
            }
            log.info("Cache invalidation event received: type={} aggregateId={}", event.type(), event.aggregateId());
            if (cacheManager != null) {
                var cache = cacheManager.getCache("catalogo");
                if (cache != null) {
                    cache.clear();
                }
            }
        } finally {
            MDC.remove("correlationId");
            MDC.remove("service");
        }
    }
}
