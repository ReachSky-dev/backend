package pl.reachsky.backend.catalog.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListingTest {

    private static final UUID SELLER = UUID.randomUUID();
    private static final ResourceWindow WINDOW = new ResourceWindow(
            Instant.parse("2027-06-01T14:00:00Z"),
            Instant.parse("2027-06-02T10:00:00Z"));

    @Test
    void newListingIsInDraftStatus() {
        Listing listing = Listing.create(SELLER, "Test room", "desc", WINDOW, 1);
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.DRAFT);
    }

    @Test
    void publishTransitionsDraftToActive() {
        Listing listing = Listing.create(SELLER, "Test room", "desc", WINDOW, 1);
        listing.publish();
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.ACTIVE);
    }

    @Test
    void publishActiveListingThrowsDomainException() {
        Listing listing = Listing.create(SELLER, "Test room", "desc", WINDOW, 1);
        listing.publish();
        assertThatThrownBy(listing::publish)
                .isInstanceOf(IllegalListingTransition.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    void blankTitleIsRejected() {
        assertThatThrownBy(() -> Listing.create(SELLER, "  ", "desc", WINDOW, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroCapacityIsRejected() {
        assertThatThrownBy(() -> Listing.create(SELLER, "Title", "desc", WINDOW, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resourceWindowRejectsEndBeforeStart() {
        Instant start = Instant.parse("2027-06-02T10:00:00Z");
        Instant end = Instant.parse("2027-06-01T14:00:00Z");
        assertThatThrownBy(() -> new ResourceWindow(start, end))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
