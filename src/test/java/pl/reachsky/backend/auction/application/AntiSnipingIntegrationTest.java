package pl.reachsky.backend.auction.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.reachsky.backend.AbstractIntegrationTest;
import pl.reachsky.backend.auction.application.port.in.BidResult;
import pl.reachsky.backend.auction.application.port.in.PlaceBidCommand;
import pl.reachsky.backend.auction.application.port.in.PlaceBidUseCase;
import pl.reachsky.backend.auction.application.port.out.AuctionRepository;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.BidderId;
import pl.reachsky.backend.shared.Money;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ChronoField;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies anti-sniping: a bid within the configured window extends endsAt.
 * Uses a controllable Clock to avoid real-time dependency.
 */
@SpringBootTest
class AntiSnipingIntegrationTest extends AbstractIntegrationTest {

    /** Mutable clock shared with the Spring context via TestConfiguration. */
    static final AtomicReference<Instant> currentTime = new AtomicReference<>(Instant.now());

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock testClock() {
            return new Clock() {
                @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
                @Override public Clock withZone(java.time.ZoneId zone) { return this; }
                @Override public Instant instant() { return currentTime.get(); }
            };
        }
    }

    @Autowired
    PlaceBidUseCase placeBidUseCase;

    @Autowired
    AuctionRepository auctionRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    UUID auctionId;
    Instant auctionEndsAt;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE bids");
        jdbcTemplate.execute("TRUNCATE TABLE proxy_bids");
        jdbcTemplate.execute("TRUNCATE TABLE auctions CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE listings CASCADE");

        Instant starts = Instant.now().minus(1, ChronoUnit.HOURS);
        auctionEndsAt = Instant.now().plus(3, ChronoUnit.MINUTES)
                .truncatedTo(ChronoUnit.MICROS); // PG timestamptz precision
        currentTime.set(Instant.now());

        UUID sellerId = UUID.randomUUID();
        UUID listingId = insertListing(sellerId);
        auctionId = insertRunningEnglishAuctionWithAntiSniping(listingId, sellerId, starts, auctionEndsAt);
    }

    @Test
    void bidWithinSnipingWindow_extendsEndsAt() {
        // Set clock to 2 minutes before end — within 5-min anti-sniping window
        Instant bidTime = auctionEndsAt.minus(2, ChronoUnit.MINUTES);
        currentTime.set(bidTime);

        BidResult result = placeBidUseCase.place(new PlaceBidCommand(
                new AuctionId(auctionId),
                new BidderId(UUID.randomUUID()),
                Money.of(10_000, "PLN"),
                "snipe-key-1"));

        assertThat(result).isInstanceOf(BidResult.Accepted.class);

        var auction = auctionRepository.findById(new AuctionId(auctionId)).orElseThrow();
        assertThat(auction.getEndsAt()).isAfter(auctionEndsAt);
        assertThat(auction.getExtensionsUsed()).isEqualTo(1);
    }

    @Test
    void bidOutsideSnipingWindow_doesNotExtend() {
        // Set clock to 10 minutes before end — outside 5-min anti-sniping window
        Instant bidTime = auctionEndsAt.minus(10, ChronoUnit.MINUTES);
        // But auction starts 1h ago and ends 3 min from now → bid at starts+1min is outside window
        Instant safeTime = auctionEndsAt.minus(8, ChronoUnit.MINUTES);
        currentTime.set(safeTime);

        placeBidUseCase.place(new PlaceBidCommand(
                new AuctionId(auctionId),
                new BidderId(UUID.randomUUID()),
                Money.of(10_000, "PLN"),
                "outside-window-key"));

        var auction = auctionRepository.findById(new AuctionId(auctionId)).orElseThrow();
        assertThat(auction.getEndsAt()).isEqualTo(auctionEndsAt);
        assertThat(auction.getExtensionsUsed()).isEqualTo(0);
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

    private UUID insertRunningEnglishAuctionWithAntiSniping(UUID listingId, UUID sellerId,
                                                              Instant starts, Instant ends) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO auctions (id, listing_id, seller_id, type, status,
                    starts_at, ends_at,
                    start_price_amount, start_price_currency,
                    min_increment_amount, min_increment_currency,
                    reserve_price_amount, reserve_price_currency,
                    current_price_amount, current_price_currency,
                    anti_sniping_window_s, anti_sniping_ext_s, anti_sniping_max_ext,
                    bid_count, extensions_used, created_at)
                VALUES (?, ?, ?, 'ENGLISH', 'RUNNING',
                    ?, ?,
                    10000, 'PLN', 500, 'PLN', 5000, 'PLN',
                    10000, 'PLN',
                    300, 300, 3,
                    0, 0, now())
                """, id, listingId, sellerId,
                java.sql.Timestamp.from(starts), java.sql.Timestamp.from(ends));
        return id;
    }
}
