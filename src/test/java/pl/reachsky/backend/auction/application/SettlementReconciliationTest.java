package pl.reachsky.backend.auction.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.reachsky.backend.AbstractIntegrationTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that reconciliation on startup finds RUNNING auctions with past endsAt
 * and settles them correctly.
 */
@SpringBootTest
class SettlementReconciliationTest extends AbstractIntegrationTest {

    @Autowired
    AuctionLifecycleService lifecycleService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    UUID auctionId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE outbox_events");
        jdbcTemplate.execute("TRUNCATE TABLE orders");
        jdbcTemplate.execute("TRUNCATE TABLE bids CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proxy_bids");
        jdbcTemplate.execute("TRUNCATE TABLE auctions CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE listings CASCADE");

        UUID sellerId = UUID.randomUUID();
        UUID listingId = insertListing(sellerId);
        auctionId = insertEndedRunningAuction(listingId, sellerId);
        insertBid(auctionId, UUID.randomUUID(), 10_000, 1);
    }

    @Test
    void runningAuctionWithPastEndsAt_isSettledByReconciliation() {
        assertThat(auctionStatus(auctionId)).isEqualTo("RUNNING");

        lifecycleService.settleDueAuctions(Instant.now());

        assertThat(auctionStatus(auctionId)).isEqualTo("SOLD");
        assertThat(countOutboxEvents(auctionId.toString())).isEqualTo(1);
    }

    @Test
    void reconciliation_isIdempotent() {
        lifecycleService.settleDueAuctions(Instant.now());
        lifecycleService.settleDueAuctions(Instant.now());
        lifecycleService.settleDueAuctions(Instant.now());

        assertThat(countOutboxEvents(auctionId.toString()))
                .as("idempotent — only one outbox event")
                .isEqualTo(1);
    }

    // -------------------------------------------------------------------------

    private String auctionStatus(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM auctions WHERE id = ?::uuid", String.class, id.toString());
    }

    private int countOutboxEvents(String aggregateId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ?",
                Integer.class, aggregateId);
        return count != null ? count : 0;
    }

    private UUID insertListing(UUID sellerId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO listings (id, seller_id, title, window_starts_at, window_ends_at, capacity, status, created_at)
                VALUES (?, ?, 'Settlement test listing', now(), now() + interval '7 day', 1, 'ACTIVE', now())
                """, id, sellerId);
        return id;
    }

    private UUID insertEndedRunningAuction(UUID listingId, UUID sellerId) {
        UUID id = UUID.randomUUID();
        Instant starts = Instant.now().minus(4, ChronoUnit.HOURS);
        Instant ends = Instant.now().minus(1, ChronoUnit.HOURS);
        jdbcTemplate.update("""
                INSERT INTO auctions (id, listing_id, seller_id, type, status,
                    starts_at, ends_at,
                    start_price_amount, start_price_currency,
                    min_increment_amount, min_increment_currency,
                    reserve_price_amount, reserve_price_currency,
                    current_price_amount, current_price_currency,
                    bid_count, extensions_used, created_at)
                VALUES (?, ?, ?, 'ENGLISH', 'RUNNING',
                    ?, ?,
                    10000, 'PLN',
                    500, 'PLN',
                    5000, 'PLN',
                    10000, 'PLN',
                    0, 0, now())
                """, id, listingId, sellerId,
                java.sql.Timestamp.from(starts), java.sql.Timestamp.from(ends));
        return id;
    }

    private void insertBid(UUID auctionId, UUID bidderId, long amount, int sequence) {
        jdbcTemplate.update(
                "UPDATE auctions SET current_price_amount = ?, bid_count = ?, highest_bidder_id = ?::uuid WHERE id = ?::uuid",
                amount, sequence, bidderId.toString(), auctionId.toString());
        UUID bidId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO bids (id, auction_id, bidder_id, amount, currency, sequence, placed_at, idempotency_key)
                VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'PLN', ?, now(), ?)
                """, bidId, auctionId, bidderId, amount, sequence, "reconcile-test-" + sequence);
    }
}
