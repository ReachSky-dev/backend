package pl.reachsky.backend.platform.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
class OutboxJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 255)
    String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    String eventType;

    @Column(nullable = false, columnDefinition = "text")
    String payload;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "timestamptz")
    Instant occurredAt;

    @Column(name = "published_at", columnDefinition = "timestamptz")
    Instant publishedAt;

    @Column(nullable = false)
    int attempts;

    OutboxJpaEntity() {}
}
