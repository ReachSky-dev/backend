package pl.reachsky.backend.auction.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.reachsky.backend.AbstractIntegrationTest;
import pl.reachsky.backend.auction.application.port.out.AuctionRepository;
import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.AuctionStatus;
import pl.reachsky.backend.auction.domain.AuctionType;
import pl.reachsky.backend.auction.domain.DutchPricing;
import pl.reachsky.backend.shared.Money;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuctionLifecycleServiceTest extends AbstractIntegrationTest {

    @Autowired
    AuctionLifecycleService lifecycleService;

    @Autowired
    AuctionRepository auctionRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE auctions");
        jdbcTemplate.execute("TRUNCATE TABLE listings CASCADE");
    }

    @Test
    void scheduledAuctionWithPastStartsAt_transitionsToRunning() {
        UUID listingId = insertListing();
        AuctionId id = insertScheduledAuction(listingId, Instant.now().minus(1, ChronoUnit.HOURS));

        lifecycleService.startDueAuctions(Instant.now());

        Auction updated = auctionRepository.findById(id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AuctionStatus.RUNNING);
    }

    @Test
    void startDueAuctions_isIdempotent() {
        UUID listingId = insertListing();
        AuctionId id = insertScheduledAuction(listingId, Instant.now().minus(1, ChronoUnit.HOURS));

        lifecycleService.startDueAuctions(Instant.now());
        lifecycleService.startDueAuctions(Instant.now());

        Auction updated = auctionRepository.findById(id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AuctionStatus.RUNNING);
    }

    @Test
    void futureScheduledAuction_remainsScheduled() {
        UUID listingId = insertListing();
        AuctionId id = insertScheduledAuction(listingId, Instant.now().plus(1, ChronoUnit.HOURS));

        lifecycleService.startDueAuctions(Instant.now());

        Auction updated = auctionRepository.findById(id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AuctionStatus.SCHEDULED);
    }

    private UUID insertListing() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO listings (id, seller_id, title, window_starts_at, window_ends_at, capacity, status, created_at)
                VALUES (?, ?, 'Test listing', now(), now() + interval '1 day', 1, 'ACTIVE', now())
                """, id, UUID.randomUUID());
        return id;
    }

    private AuctionId insertScheduledAuction(UUID listingId, Instant startsAt) {
        Instant endsAt = startsAt.plus(24, ChronoUnit.HOURS);
        Auction auction = Auction.reconstitute(
                new AuctionId(UUID.randomUUID()), listingId, UUID.randomUUID(),
                AuctionType.DUTCH, AuctionStatus.SCHEDULED,
                startsAt, endsAt,
                new DutchPricing(
                        Money.of(10_000, "PLN"),
                        Money.of(1_000, "PLN"),
                        Duration.ofHours(1),
                        Money.of(3_000, "PLN")),
                Money.of(3_000, "PLN"), null, Instant.now());
        auctionRepository.save(auction);
        return auction.getId();
    }
}
