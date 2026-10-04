package pl.reachsky.backend.platform.outbox;

import pl.reachsky.backend.shared.Ids;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox event. Saved in the same transaction as the aggregate state change.
 * The OutboxPoller publishes it asynchronously after the transaction commits.
 */
public final class OutboxEvent {

    private final UUID id;
    private final String aggregateType;
    private final String aggregateId;
    private final String eventType;
    private final String payload;
    private final Instant occurredAt;
    private Instant publishedAt;
    private int attempts;

    private OutboxEvent(UUID id, String aggregateType, String aggregateId,
                        String eventType, String payload, Instant occurredAt,
                        Instant publishedAt, int attempts) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.publishedAt = publishedAt;
        this.attempts = attempts;
    }

    public static OutboxEvent create(String aggregateType, String aggregateId,
                                     String eventType, String payload, Instant occurredAt) {
        return new OutboxEvent(Ids.next(), aggregateType, aggregateId,
                eventType, payload, occurredAt, null, 0);
    }

    public static OutboxEvent reconstitute(UUID id, String aggregateType, String aggregateId,
                                           String eventType, String payload, Instant occurredAt,
                                           Instant publishedAt, int attempts) {
        return new OutboxEvent(id, aggregateType, aggregateId, eventType, payload,
                occurredAt, publishedAt, attempts);
    }

    public void markPublished(Instant now) {
        this.publishedAt = now;
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public int getAttempts() { return attempts; }
}
