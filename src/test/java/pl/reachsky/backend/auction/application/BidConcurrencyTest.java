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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies invariant I1: exactly one bid wins when N threads compete simultaneously.
 *
 * All 20 threads try to be the FIRST bidder at startPrice (10 000 PLN).
 * With pessimistic locking on the auction row, only the thread that acquires
 * the lock first can succeed — the others read bidCount=1 afterwards and fail
 * with BidTooLow (minimum is now startPrice + increment = 10 500 PLN).
 */
@SpringBootTest
class BidConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    PlaceBidUseCase placeBidUseCase;

    @Autowired
    JdbcTemplate jdbcTemplate;

    UUID auctionId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE bids");
        jdbcTemplate.execute("TRUNCATE TABLE proxy_bids");
        jdbcTemplate.execute("TRUNCATE TABLE auctions CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE listings CASCADE");

        UUID sellerId = UUID.randomUUID();
        UUID listingId = insertListing(sellerId);
        auctionId = insertRunningEnglishAuction(listingId, sellerId);
    }

    @Test
    void withPessimisticLock_exactlyOneBidWins_outOf20Concurrent() throws InterruptedException {
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        List<BidResult> results = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger exceptions = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int bidderIndex = i;
            executor.submit(() -> {
                try {
                    startGate.await();
                    PlaceBidCommand cmd = new PlaceBidCommand(
                            new AuctionId(auctionId),
                            new BidderId(UUID.randomUUID()),
                            Money.of(10_000, "PLN"),         // startPrice — valid for the 1st bid only
                            "concurrency-test-key-" + bidderIndex);
                    results.add(placeBidUseCase.place(cmd));
                } catch (Exception e) {
                    exceptions.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        List<BidResult> accepted = results.stream()
                .filter(r -> r instanceof BidResult.Accepted).toList();

        System.out.println("=== BidConcurrencyTest (WITH pessimistic lock) ===");
        System.out.println("Total results:   " + results.size());
        System.out.println("Accepted:        " + accepted.size());
        System.out.println("Domain rejected: " + exceptions.get());
        System.out.println("=================================================");

        assertThat(accepted).as("exactly one bid must win").hasSize(1);
        assertThat(exceptions.get() + results.size())
                .as("all threads must terminate").isEqualTo(threads);
    }

    // -------------------------------------------------------------------------

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
                    10000, 'PLN',
                    500, 'PLN',
                    5000, 'PLN',
                    10000, 'PLN',
                    0, 0, now())
                """, id, listingId, sellerId,
                java.sql.Timestamp.from(starts), java.sql.Timestamp.from(ends));
        return id;
    }
}
