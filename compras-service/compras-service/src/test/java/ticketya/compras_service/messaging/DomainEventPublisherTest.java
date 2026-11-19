package ticketya.compras_service.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomainEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private DomainEventPublisher publisher;

    @Test
    void publicacionEvento_enviaAlExchange() {
        DomainEvent event = DomainEvent.of("pago.aprobado", "orden-123", "payload", "corr-456");
        publisher.publish(event);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE),
                eq("pago.aprobado"),
                eq(event),
                any(MessagePostProcessor.class)
        );
    }

    @Test
    void eventoDuplicadoProcesadoUnaVez() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(startsWith("seen:metricas.q:"), eq("1"), any(Duration.class)))
                .thenReturn(Boolean.TRUE)  // primer evento: nuevo
                .thenReturn(Boolean.FALSE); // segundo evento: duplicado

        MetricasConsumer consumer = new MetricasConsumer(redisTemplate);

        DomainEvent event = DomainEvent.of("pago.aprobado", "orden-123", "payload");

        // Primer procesamiento
        consumer.handleMetricasEvent(event);

        // Segundo procesamiento (duplicado)
        consumer.handleMetricasEvent(event);

        verify(valueOperations, times(2)).setIfAbsent(eq("seen:metricas.q:" + event.eventId()), eq("1"), any(Duration.class));
    }
}
