package pl.reachsky.backend.platform.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository {

    void save(OutboxEvent event);

    /** Returns up to {@code limit} unpublished events ordered by occurredAt ASC. */
    List<OutboxEvent> findUnpublished(int limit);

    void markPublished(UUID id, Instant publishedAt);

    void incrementAttempts(UUID id);

    List<OutboxEvent> findAll();
}
