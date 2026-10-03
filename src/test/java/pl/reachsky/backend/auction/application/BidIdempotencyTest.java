package pl.reachsky.backend.auction.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.reachsky.backend.AbstractIntegrationTest;
import pl.reachsky.backend.auction.application.port.in.BidResult;
import pl.reachsky.backend.auction.application.port.in.PlaceBidCommand;
import pl.reachsky.backend.auction.application.port.in.PlaceBidUseCase;
import pl.reachsky.backend.auction.application.port.out.BidRepository;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.BidderId;
import pl.reachsky.backend.shared.Money;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies invariant I9: duplicate idempotency key always produces the same bid.
 */
@SpringBootTest
class BidIdempotencyTest extends AbstractIntegrationTest {

    @Autowired
    PlaceBidUseCase placeBidUseCase;

    @Autowired
    BidRepository bidRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    UUID auctionId;
    UUID bidderId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE bids");
        jdbcTemplate.execute("TRUNCATE TABLE proxy_bids");
        jdbcTemplate.execute("TRUNCATE TABLE auctions CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE listings CASCADE");

        UUID sellerId = UUID.randomUUID();
        bidderId = UUID.randomUUID();
        UUID listingId = insertListing(sellerId);
        auctionId = insertRunningEnglishAuction(listingId, sellerId);
    }

    @Test
    void sameBidSentTwice_secondCallReturnsDuplicate() {
        String key = "idem-key-" + UUID.randomUUID();
        PlaceBidCommand cmd = command(key);

        BidResult first = placeBidUseCase.place(cmd);
        BidResult second = placeBidUseCase.place(cmd);

        assertThat(first).isInstanceOf(BidResult.Accepted.class);
        assertThat(second).isInstanceOf(BidResult.Duplicate.class);

        UUID firstId = ((BidResult.Accepted) first).bid().getId().value();
        UUID secondId = ((BidResult.Duplicate) second).bid().getId().value();
        assertThat(firstId).isEqualTo(secondId);
    }

    @Test
    void concurrentSameKey_exactlyOneBidCreated() throws InterruptedException {
        String key = "concurrent-idem-" + UUID.randomUUID();
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<BidResult> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    results.add(placeBidUseCase.place(command(key)));
                } catch (Exception ignored) {
                    // first-bid BidTooLow from 2nd+ concurrent accepted bids (idempotency key makes them duplicates)
                } finally {
                    done.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        long accepted = results.stream().filter(r -> r instanceof BidResult.Accepted).count();
        long duplicates = results.stream().filter(r -> r instanceof BidResult.Duplicate).count();

        assertThat(accepted).as("exactly one accepted").isEqualTo(1);
        assertThat(accepted + duplicates).as("all results accounted for").isEqualTo(results.size());

        long bidsInDb = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bids WHERE idempotency_key = ?", Long.class, key);
        assertThat(bidsInDb).as("only one row in DB").isEqualTo(1);
    }

    // -------------------------------------------------------------------------

    private PlaceBidCommand command(String key) {
        return new PlaceBidCommand(
                new AuctionId(auctionId),
                new BidderId(bidderId),
                Money.of(10_000, "PLN"),
                key);
    }

    private UUID insertListing(UUID sellerId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO listings (id, seller_id, title, window_starts_at, window_ends_at, capacity, status, created_at)
                VALUES (?, ?, 'Test listing', now(), now() + interval '7 day', 1, 'ACTIVE', now())
                """, id, sellerId);
        return id;
    }

    private UUID insertRunningEnglishAuction(UUID listingId, UUID sellerId) {
        UUID id = UUID.randomUUID();
        Instant starts = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant ends = Instant.now().plus(2, ChronoUnit.HOURS);
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
                    10000, 'PLN', 500, 'PLN', 5000, 'PLN',
                    10000, 'PLN', 0, 0, now())
                """, id, listingId, sellerId,
                java.sql.Timestamp.from(starts), java.sql.Timestamp.from(ends));
        return id;
    }
}
