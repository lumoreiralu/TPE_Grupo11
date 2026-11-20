package com.ticketya.entradas.messaging;

import com.ticketya.entradas.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class NotificacionesConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificacionesConsumer.class);
    private static final String QUEUE = RabbitMQConfig.NOTIFICACIONES_Q;

    private final StringRedisTemplate redisTemplate;
    private final NotificationService notificationService;

    public NotificacionesConsumer(StringRedisTemplate redisTemplate, NotificationService notificationService) {
        this.redisTemplate = redisTemplate;
        this.notificationService = notificationService;
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

        String dedupeKey = "seen:" + QUEUE + ":" + event.eventId();
        try {
            Boolean isNew = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", Duration.ofHours(24));
            if (Boolean.FALSE.equals(isNew)) {
                log.info("Duplicate event ignored: eventId={} type={}", event.eventId(), event.type());
                return;
            }

            log.info("Notification event received: type={} aggregateId={}", event.type(), event.aggregateId());
            procesar(event);
        } catch (RuntimeException error) {
            // Si el correo falla, se libera la marca de deduplicación para que
            // el reintento de RabbitMQ vuelva a procesar el evento.
            redisTemplate.delete(dedupeKey);
            log.error("Error processing notification event: eventId={} type={}", event.eventId(), event.type(), error);
            throw error;
        } finally {
            MDC.clear();
        }
    }

    public void handleNotificacion(DomainEvent event) {
        handleNotificacion(event, null);
    }

    private void procesar(DomainEvent event) {
        Map<String, Object> payload = comoMapa(event.payload());
        String tipo = event.type() != null ? event.type() : "";

        switch (tipo) {
            case "pago.aprobado" -> {
                String ordenId = texto(payload, "ordenId", event.aggregateId());
                String clienteId = texto(payload, "clienteId", null);
                String email = texto(payload, "email", texto(payload, "emailCliente", null));
                notificationService.enviarConfirmacion(clienteId, email, ordenId);
            }
            case "devolucion.aprobada" -> {
                String ordenId = texto(payload, "ordenId", event.aggregateId());
                String clienteId = texto(payload, "clienteId", null);
                String email = texto(payload, "email", texto(payload, "emailCliente", null));
                String motivo = texto(payload, "motivo", texto(payload, "detalle", null));
                notificationService.enviarDevolucionAprobada(clienteId, email, ordenId, motivo);
            }
            case "evento.cancelado" -> {
                String eventoId = eventoId(event, payload);
                String nombre = texto(payload, "nombre",
                        texto(payload, "nombreEvento", "Evento " + eventoId));
                String detalle = texto(payload, "detalle",
                        "El evento fue cancelado y tus entradas serán reintegradas.");
                notificationService.enviarAvisoEvento(
                        eventoId, "TicketYa - Evento cancelado: " + nombre, detalle);
            }
            case "evento.modificado" -> {
                String eventoId = eventoId(event, payload);
                String nombre = texto(payload, "nombre",
                        texto(payload, "nombreEvento", "Evento " + eventoId));
                String detalle = texto(payload, "detalle",
                        texto(payload, "cambios",
                                "El evento cambió de horario, lugar o programación."));
                notificationService.enviarAvisoEvento(
                        eventoId, "TicketYa - Evento modificado: " + nombre, detalle);
            }
            default -> log.info("Notification event without handler: type={} eventId={}",
                    event.type(), event.eventId());
        }
    }

    private String eventoId(DomainEvent event, Map<String, Object> payload) {
        return texto(payload, "eventoId", event.aggregateId());
    }

    private Map<String, Object> comoMapa(Object payload) {
        if (payload instanceof Map<?, ?> mapa) {
            Map<String, Object> resultado = new LinkedHashMap<>();
            mapa.forEach((clave, valor) -> {
                if (clave != null) {
                    resultado.put(String.valueOf(clave), valor);
                }
            });
            return resultado;
        }
        return Map.of();
    }

    private String texto(Map<String, Object> payload, String clave, String porDefecto) {
        Object valor = payload.get(clave);
        if (valor == null) {
            return porDefecto;
        }
        String texto = String.valueOf(valor);
        return texto.isBlank() ? porDefecto : texto;
    }
}
