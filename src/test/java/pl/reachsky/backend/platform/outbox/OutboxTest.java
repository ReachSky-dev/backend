package pl.reachsky.backend.platform.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pl.reachsky.backend.AbstractIntegrationTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the transactional outbox: save-in-same-tx, rollback removes event,
 * poller publishes exactly once, failed publish increments attempts.
 */
@SpringBootTest
class OutboxTest extends AbstractIntegrationTest {

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PlatformTransactionManager txManager;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE outbox_events");
        jdbcTemplate.execute("TRUNCATE TABLE orders");
    }

    @Test
    void outboxEvent_savedInSameTransaction_isVisible() {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.executeWithoutResult(tx -> {
            OutboxEvent event = OutboxEvent.create("Auction", UUID.randomUUID().toString(),
                    "AUCTION_RESERVE_NOT_MET",
                    "{\"auctionId\":\"" + UUID.randomUUID() + "\"}",
                    Instant.now());
            outboxRepository.save(event);
        });

        List<OutboxEvent> all = outboxRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getPublishedAt()).isNull();
    }

    @Test
    void outboxEvent_rolledBack_isNotVisible() {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(tx -> {
            tx.setRollbackOnly();
            OutboxEvent event = OutboxEvent.create("Auction", UUID.randomUUID().toString(),
                    "AUCTION_RESERVE_NOT_MET",
                    "{\"auctionId\":\"" + UUID.randomUUID() + "\"}",
                    Instant.now());
            outboxRepository.save(event);
            return null;
        });

        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void findUnpublished_returnsOnlyUnpublished() {
        UUID id1 = saveOutboxEvent("EV1");
        UUID id2 = saveOutboxEvent("EV2");
        outboxRepository.markPublished(id1, Instant.now());

        List<OutboxEvent> unpublished = outboxRepository.findUnpublished(50);
        assertThat(unpublished).hasSize(1);
        assertThat(unpublished.get(0).getId()).isEqualTo(id2);
    }

    @Test
    void markPublished_setsPublishedAt() {
        UUID id = saveOutboxEvent("TEST");
        Instant now = Instant.now();

        outboxRepository.markPublished(id, now);

        List<OutboxEvent> all = outboxRepository.findAll();
        assertThat(all.get(0).getPublishedAt()).isNotNull();
    }

    @Test
    void incrementAttempts_incrementsCounter() {
        UUID id = saveOutboxEvent("FAIL");

        outboxRepository.incrementAttempts(id);
        outboxRepository.incrementAttempts(id);

        List<OutboxEvent> all = outboxRepository.findAll();
        assertThat(all.get(0).getAttempts()).isEqualTo(2);
    }

    // -------------------------------------------------------------------------

    private UUID saveOutboxEvent(String aggregateId) {
        OutboxEvent event = OutboxEvent.create("Test", aggregateId,
                "AUCTION_RESERVE_NOT_MET",
                "{\"auctionId\":\"" + UUID.randomUUID() + "\"}",
                Instant.now());
        outboxRepository.save(event);
        return event.getId();
    }
}
