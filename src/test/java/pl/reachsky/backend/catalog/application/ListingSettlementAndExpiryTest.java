package pl.reachsky.backend.catalog.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.reachsky.backend.AbstractIntegrationTest;
import pl.reachsky.backend.catalog.application.port.out.ListingRepository;
import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingId;
import pl.reachsky.backend.catalog.domain.ListingStatus;
import pl.reachsky.backend.platform.outbox.AuctionReserveNotMetEvent;
import pl.reachsky.backend.platform.outbox.AuctionSoldEvent;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ListingSettlementAndExpiryTest extends AbstractIntegrationTest {

    @Autowired
    CloseListingOnAuctionEndedListener listener;

    @Autowired
    ExpireListingsService expireService;

    @Autowired
    ListingRepository listingRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE listings CASCADE");
    }

    // ── settlement ────────────────────────────────────────────────────────────

    @Test
    void onAuctionSold_listingBecomesSOLD() {
        UUID listingId = insertActiveListing("now()", "now() + interval '1 day'");

        listener.onAuctionSold(new AuctionSoldEvent(
                UUID.randomUUID(), listingId, UUID.randomUUID(), 5000L, "PLN"));

        assertStatus(listingId, ListingStatus.SOLD);
    }

    @Test
    void onAuctionReserveNotMet_listingBecomesCLOSED() {
        UUID listingId = insertActiveListing("now()", "now() + interval '1 day'");

        listener.onAuctionReserveNotMet(new AuctionReserveNotMetEvent(UUID.randomUUID(), listingId));

        assertStatus(listingId, ListingStatus.CLOSED);
    }

    // ── expiry ────────────────────────────────────────────────────────────────

    @Test
    void expireDueListings_expiresActiveListingPastWindowEnd() {
        UUID listingId = insertActiveListing("now() - interval '2 days'", "now() - interval '1 day'");

        expireService.expireDueListings(Instant.now());

        assertStatus(listingId, ListingStatus.EXPIRED);
    }

    @Test
    void expireDueListings_doesNotExpireListingWithFutureWindow() {
        UUID listingId = insertActiveListing("now() + interval '1 day'", "now() + interval '2 days'");

        expireService.expireDueListings(Instant.now());

        assertStatus(listingId, ListingStatus.ACTIVE);
    }

    @Test
    void expireDueListings_doesNotExpireListingWithRunningAuction() {
        UUID listingId = insertActiveListing("now() - interval '2 days'", "now() - interval '1 day'");
        insertAuction(listingId, "RUNNING");

        expireService.expireDueListings(Instant.now());

        assertStatus(listingId, ListingStatus.ACTIVE);
    }

    @Test
    void expireDueListings_doesNotExpireListingWithScheduledAuction() {
        UUID listingId = insertActiveListing("now() - interval '2 days'", "now() - interval '1 day'");
        insertAuction(listingId, "SCHEDULED");

        expireService.expireDueListings(Instant.now());

        assertStatus(listingId, ListingStatus.ACTIVE);
    }

    @Test
    void expireDueListings_isIdempotent() {
        UUID listingId = insertActiveListing("now() - interval '2 days'", "now() - interval '1 day'");

        expireService.expireDueListings(Instant.now());
        expireService.expireDueListings(Instant.now());

        assertStatus(listingId, ListingStatus.EXPIRED);
    }

    @Test
    void expireDueListings_handlesListingsExpiredWhileAppWasDown() {
        // Simulates startup reconciliation: listing expired before this call.
        UUID listingId = insertActiveListing("now() - interval '10 days'", "now() - interval '9 days'");

        expireService.expireDueListings(Instant.now());

        assertStatus(listingId, ListingStatus.EXPIRED);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private UUID insertActiveListing(String windowStart, String windowEnd) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO listings (id, seller_id, title, window_starts_at, window_ends_at,
                    capacity, status, created_at)
                VALUES (?, gen_random_uuid(), 'Test listing', %s, %s, 1, 'ACTIVE', now())
                """.formatted(windowStart, windowEnd), id);
        return id;
    }

    private void insertAuction(UUID listingId, String status) {
        jdbcTemplate.update("""
                INSERT INTO auctions (id, listing_id, seller_id, type, status,
                    starts_at, ends_at,
                    start_price_amount, start_price_currency,
                    current_price_amount, current_price_currency,
                    min_increment_amount, min_increment_currency,
                    reserve_price_amount, reserve_price_currency)
                VALUES (gen_random_uuid(), ?, gen_random_uuid(), 'ENGLISH', ?,
                    now() - interval '1 hour', now() + interval '1 day',
                    10000, 'PLN', 10000, 'PLN', 500, 'PLN', 8000, 'PLN')
                """, listingId, status);
    }

    private void assertStatus(UUID listingId, ListingStatus expected) {
        Listing listing = listingRepository.findById(new ListingId(listingId)).orElseThrow();
        assertThat(listing.getStatus()).isEqualTo(expected);
    }
}
