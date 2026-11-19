package ticketya.compras_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class MetricasConsumer {
    private static final Logger log = LoggerFactory.getLogger(MetricasConsumer.class);
    private final StringRedisTemplate redisTemplate;

    public MetricasConsumer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.METRICAS_Q)
    public void handleMetricasEvent(DomainEvent event,
                                    @Header(name = "X-Correlation-ID", required = false) String headerCorrelationId) {
        restoreCorrelationId(event, headerCorrelationId);
        try {
            String dedupeKey = "seen:metricas.q:" + event.eventId();
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", Duration.ofHours(24));
            if (Boolean.FALSE.equals(isNew)) {
                log.info("Duplicate event ignored: eventId={}", event.eventId());
                return;
            }
            log.info("Metricas event received: type={} aggregateId={}", event.type(), event.aggregateId());
        } finally {
            MDC.clear();
        }
    }

    /**
     * El header del mensaje es la fuente principal; el payload queda como
     * respaldo para mensajes publicados sin header.
     */
    public void handleMetricasEvent(DomainEvent event) {
        handleMetricasEvent(event, null);
    }

    private void restoreCorrelationId(DomainEvent event, String headerCorrelationId) {
        String correlationId = (headerCorrelationId != null && !headerCorrelationId.isBlank())
                ? headerCorrelationId
                : event.correlationId();
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        MDC.put("service", "compras-service");
    }
}
