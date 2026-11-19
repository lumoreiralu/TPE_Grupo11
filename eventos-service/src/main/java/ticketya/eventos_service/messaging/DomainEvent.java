package ticketya.eventos_service.messaging;

import org.slf4j.MDC;

import java.time.Instant;
import java.util.UUID;

public record DomainEvent(
    String eventId,
    String type,
    String aggregateId,
    Object payload,
    String correlationId,
    Instant occurredAt
) {
    private static final String MDC_CORRELATION_ID_KEY = "correlationId";

    public static DomainEvent of(String type, String aggregateId, Object payload) {
        return of(type, aggregateId, payload, MDC.get(MDC_CORRELATION_ID_KEY));
    }
    public static DomainEvent of(String type, String aggregateId, Object payload, String correlationId) {
        return new DomainEvent(
            UUID.randomUUID().toString(),
            type,
            aggregateId,
            payload,
            correlationId,
            Instant.now()
        );
    }
}
