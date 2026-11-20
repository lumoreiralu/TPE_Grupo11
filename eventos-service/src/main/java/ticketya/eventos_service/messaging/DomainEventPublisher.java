package ticketya.eventos_service.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public DomainEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventPublisherExecutor")
    public void handleDomainEvent(DomainEvent event) {
        publish(event);
    }

    public void publish(DomainEvent event) {
        try {
            String correlationId = MDC.get("correlationId");
            if (correlationId == null) {
                correlationId = event.correlationId();
            }
            final String finalCorrelationId = correlationId;

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, event.type(), event, message -> {
                if (finalCorrelationId != null) {
                    message.getMessageProperties().setHeader("correlationId", finalCorrelationId);
                    message.getMessageProperties().setHeader("X-Correlation-ID", finalCorrelationId);
                }
                return message;
            });
            log.info("Event published: type={} aggregateId={} eventId={} correlationId={}",
                    event.type(), event.aggregateId(), event.eventId(), finalCorrelationId);
        } catch (Exception e) {
            log.error("Failed to publish event: type={} eventId={} error={}",
                    event.type(), event.eventId(), e.getMessage());
        }
    }
}
