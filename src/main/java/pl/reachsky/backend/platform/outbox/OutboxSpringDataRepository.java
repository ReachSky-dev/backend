package pl.reachsky.backend.platform.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface OutboxSpringDataRepository extends JpaRepository<OutboxJpaEntity, UUID> {

    @Query("SELECT e FROM OutboxJpaEntity e WHERE e.publishedAt IS NULL ORDER BY e.occurredAt ASC")
    List<OutboxJpaEntity> findUnpublished(Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxJpaEntity e SET e.publishedAt = :publishedAt WHERE e.id = :id")
    void markPublished(UUID id, Instant publishedAt);

    @Modifying
    @Query("UPDATE OutboxJpaEntity e SET e.attempts = e.attempts + 1 WHERE e.id = :id")
    void incrementAttempts(UUID id);
}
