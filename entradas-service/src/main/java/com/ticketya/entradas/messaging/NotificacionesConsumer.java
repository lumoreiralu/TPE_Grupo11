package com.ticketya.entradas.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class NotificacionesConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificacionesConsumer.class);
    private final StringRedisTemplate redisTemplate;

    public NotificacionesConsumer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICACIONES_Q)
    public void handleNotificacion(DomainEvent event,
                                   @Header(name = "X-Correlation-ID", required = false) String headerCorrelationId) {
        String correlationId = (headerCorrelationId != null && !headerCorrelationId.isBlank())
                ? headerCorrelationId
                : event.correlationId();
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        MDC.put("service", "entradas-service");
        try {
            String dedupeKey = "seen:notificaciones.q:" + event.eventId();
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", Duration.ofHours(24));
            if (Boolean.FALSE.equals(isNew)) {
                log.info("Duplicate event ignored: eventId={}", event.eventId());
                return;
            }
            log.info("Notification event received: type={} aggregateId={}", event.type(), event.aggregateId());
        } finally {
            MDC.clear();
        }
    }

    public void handleNotificacion(DomainEvent event) {
        handleNotificacion(event, null);
    }
}
