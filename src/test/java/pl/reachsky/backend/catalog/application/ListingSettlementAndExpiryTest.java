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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ListingSettlementAndExpiryTest extends AbstractIntegrationTest {

    @Autowired
    CloseListingOnAuctionEndedListener listener;

    @Autowired
    CloseExpiredListingsService expiryService;

    @Autowired
    ListingRepository listingRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE listings CASCADE");
    }

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

    @Test
    void closeExpired_closesActiveListingWithExpiredWindow() {
        UUID listingId = insertActiveListing("now() - interval '2 days'", "now() - interval '1 day'");

        expiryService.closeExpired();

        assertStatus(listingId, ListingStatus.CLOSED);
    }

    @Test
    void closeExpired_doesNotCloseListingWithFutureWindow() {
        UUID listingId = insertActiveListing("now() + interval '1 day'", "now() + interval '2 days'");

        expiryService.closeExpired();

        assertStatus(listingId, ListingStatus.ACTIVE);
    }

    // -------------------------------------------------------------------------

    private UUID insertActiveListing(String windowStart, String windowEnd) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO listings (id, seller_id, title, window_starts_at, window_ends_at,
                    capacity, status, created_at)
                VALUES (?, gen_random_uuid(), 'Test listing', %s, %s, 1, 'ACTIVE', now())
                """.formatted(windowStart, windowEnd), id);
        return id;
    }

    private void assertStatus(UUID listingId, ListingStatus expected) {
        Listing listing = listingRepository.findById(new ListingId(listingId)).orElseThrow();
        assertThat(listing.getStatus()).isEqualTo(expected);
    }
}
