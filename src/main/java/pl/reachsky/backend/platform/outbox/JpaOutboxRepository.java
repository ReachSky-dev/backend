package pl.reachsky.backend.platform.outbox;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@Transactional
class JpaOutboxRepository implements OutboxRepository {

    private final OutboxSpringDataRepository springRepo;

    JpaOutboxRepository(OutboxSpringDataRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public void save(OutboxEvent event) {
        OutboxJpaEntity e = new OutboxJpaEntity();
        e.id = event.getId();
        e.aggregateType = event.getAggregateType();
        e.aggregateId = event.getAggregateId();
        e.eventType = event.getEventType();
        e.payload = event.getPayload();
        e.occurredAt = event.getOccurredAt();
        e.publishedAt = event.getPublishedAt();
        e.attempts = event.getAttempts();
        springRepo.save(e);
    }

    @Override
    public List<OutboxEvent> findUnpublished(int limit) {
        return springRepo.findUnpublished(PageRequest.of(0, limit, Sort.by("occurredAt")))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void markPublished(UUID id, Instant publishedAt) {
        springRepo.markPublished(id, publishedAt);
    }

    @Override
    public void incrementAttempts(UUID id) {
        springRepo.incrementAttempts(id);
    }

    @Override
    public List<OutboxEvent> findAll() {
        return springRepo.findAll().stream().map(this::toDomain).toList();
    }

    private OutboxEvent toDomain(OutboxJpaEntity e) {
        return OutboxEvent.reconstitute(e.id, e.aggregateType, e.aggregateId,
                e.eventType, e.payload, e.occurredAt, e.publishedAt, e.attempts);
    }
}
