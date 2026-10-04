package pl.reachsky.backend.auction.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pl.reachsky.backend.AbstractIntegrationTest;
import pl.reachsky.backend.auction.application.port.in.SettleAuctionUseCase;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.SettlementResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies invariant I11: auction is settled exactly once, even when N threads
 * try to settle concurrently, and even after a simulated failure before commit.
 */
@SpringBootTest
class SettlementExactlyOnceTest extends AbstractIntegrationTest {

    @Autowired
    SettleAuctionUseCase settleAuction;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PlatformTransactionManager txManager;

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
        auctionId = insertEndedAuction(listingId, sellerId);
        // Insert 1 bid above reserve (reserve = 5000, bid = 10000)
        insertBid(auctionId, UUID.randomUUID(), 10_000, 1);
    }

    // -------------------------------------------------------------------------
    // Part (a): 10 concurrent settlement attempts → exactly 1 outbox event
    // -------------------------------------------------------------------------

    @Test
    void concurrentSettlement_exactlyOneOutboxEvent_auctionStatusSold() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    settleAuction.settle(new AuctionId(auctionId));
                } catch (Exception ignored) {
                    // Lock contention or AlreadySettled — expected
                } finally {
                    done.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        int outboxCount = countOutboxEvents(auctionId.toString());
        String status = auctionStatus(auctionId);

        System.out.println("=== SettlementExactlyOnceTest (a) — 10 concurrent ===");
        System.out.println("Outbox events: " + outboxCount);
        System.out.println("Auction status: " + status);
        System.out.println("====================================================");

        assertThat(outboxCount).as("exactly one outbox event").isEqualTo(1);
        assertThat(status).as("auction must be SOLD").isEqualTo("SOLD");
    }

    // -------------------------------------------------------------------------
    // Part (b): rollback before commit → no trace; reconciliation fixes it
    // -------------------------------------------------------------------------

    @Test
    void failureBeforeCommit_leavesNoTrace_reconciliationFixes() {
        // Simulate a crash: outer transaction always rolls back
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(tx -> {
            tx.setRollbackOnly();
            settleAuction.settle(new AuctionId(auctionId)); // joins outer tx, work will be rolled back
            return null;
        });

        assertThat(auctionStatus(auctionId)).as("rolled-back — still RUNNING").isEqualTo("RUNNING");
        assertThat(countOutboxEvents(auctionId.toString())).as("rolled-back — no outbox event").isEqualTo(0);

        // Reconciliation: settle again (I11)
        SettlementResult result = settleAuction.settle(new AuctionId(auctionId));

        System.out.println("=== SettlementExactlyOnceTest (b) — reconciliation ===");
        System.out.println("Result: " + result.getClass().getSimpleName());
        System.out.println("Auction status: " + auctionStatus(auctionId));
        System.out.println("Outbox events: " + countOutboxEvents(auctionId.toString()));
        System.out.println("======================================================");

        assertThat(result).isInstanceOf(SettlementResult.Sold.class);
        assertThat(auctionStatus(auctionId)).isEqualTo("SOLD");
        assertThat(countOutboxEvents(auctionId.toString())).isEqualTo(1);
    }

    // -------------------------------------------------------------------------

    private int countOutboxEvents(String aggregateId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ?",
                Integer.class, aggregateId);
        return count != null ? count : 0;
    }

    private String auctionStatus(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM auctions WHERE id = ?::uuid", String.class, id.toString());
    }

    private UUID insertListing(UUID sellerId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO listings (id, seller_id, title, window_starts_at, window_ends_at, capacity, status, created_at)
                VALUES (?, ?, 'Test listing', now(), now() + interval '7 day', 1, 'ACTIVE', now())
                """, id, sellerId);
        return id;
    }

    private UUID insertEndedAuction(UUID listingId, UUID sellerId) {
        UUID id = UUID.randomUUID();
        Instant starts = Instant.now().minus(3, ChronoUnit.HOURS);
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
        // Update auction current_price and bid_count first
        jdbcTemplate.update(
                "UPDATE auctions SET current_price_amount = ?, bid_count = ?, highest_bidder_id = ?::uuid WHERE id = ?::uuid",
                amount, sequence, bidderId.toString(), auctionId.toString());
        UUID bidId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO bids (id, auction_id, bidder_id, amount, currency, sequence, placed_at, idempotency_key)
                VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'PLN', ?, now(), ?)
                """, bidId, auctionId, bidderId, amount, sequence, "settle-test-key-" + sequence);
    }
}
