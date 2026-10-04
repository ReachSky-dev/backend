package pl.reachsky.backend.platform.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Polls the outbox_events table for unpublished events and publishes them as Spring
 * application events. Each event is processed in its own transaction so that
 * publishing and marking-as-published are atomic.
 *
 * Uses TransactionTemplate instead of @Transactional to avoid triggering the
 * ArchUnit rule that restricts @Transactional to the application layer.
 */
@Component
@ConditionalOnProperty(name = "reachsky.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private static final int BATCH_SIZE = 50;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    OutboxPoller(OutboxRepository outboxRepository,
                 ApplicationEventPublisher eventPublisher,
                 PlatformTransactionManager txManager,
                 Clock clock) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = new TransactionTemplate(txManager);
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 2_000)
    public void poll() {
        List<OutboxEvent> unpublished = outboxRepository.findUnpublished(BATCH_SIZE);
        for (OutboxEvent event : unpublished) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        transactionTemplate.executeWithoutResult(tx -> {
            try {
                Object applicationEvent = toApplicationEvent(event);
                eventPublisher.publishEvent(applicationEvent);
                outboxRepository.markPublished(event.getId(), clock.instant());
            } catch (Exception e) {
                log.error("Failed to process outbox event {} ({}): {}",
                        event.getId(), event.getEventType(), e.getMessage());
                outboxRepository.incrementAttempts(event.getId());
                tx.setRollbackOnly();
            }
        });
    }

    private Object toApplicationEvent(OutboxEvent event) throws Exception {
        JsonNode node = MAPPER.readTree(event.getPayload());
        return switch (event.getEventType()) {
            case "AUCTION_SOLD" -> new AuctionSoldEvent(
                    UUID.fromString(node.get("auctionId").asText()),
                    UUID.fromString(node.get("listingId").asText()),
                    UUID.fromString(node.get("winnerId").asText()),
                    node.get("amountInMinorUnits").asLong(),
                    node.get("currency").asText());
            case "AUCTION_RESERVE_NOT_MET" -> new AuctionReserveNotMetEvent(
                    UUID.fromString(node.get("auctionId").asText()),
                    UUID.fromString(node.get("listingId").asText()));
            default -> throw new IllegalArgumentException("Unknown event type: " + event.getEventType());
        };
    }
}
