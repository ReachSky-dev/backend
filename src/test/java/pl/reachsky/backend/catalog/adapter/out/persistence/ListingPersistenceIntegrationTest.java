package pl.reachsky.backend.catalog.adapter.out.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.reachsky.backend.AbstractIntegrationTest;
import pl.reachsky.backend.catalog.application.port.out.ListingRepository;
import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingStatus;
import pl.reachsky.backend.catalog.domain.ResourceWindow;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ListingPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ListingRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private static final UUID SELLER = UUID.randomUUID();
    private static final ResourceWindow WINDOW = new ResourceWindow(
            Instant.parse("2027-07-01T12:00:00Z"),
            Instant.parse("2027-07-02T10:00:00Z"));

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.execute("TRUNCATE TABLE listings");
    }

    @Test
    void savesAndReloadsListing() {
        Listing listing = Listing.create(SELLER, "Hotel room", "Nice view", WINDOW, 2);
        repository.save(listing);

        Optional<Listing> found = repository.findById(listing.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Hotel room");
        assertThat(found.get().getStatus()).isEqualTo(ListingStatus.DRAFT);
        assertThat(found.get().getCapacity()).isEqualTo(2);
    }

    @Test
    void savesUpdatedStatusAfterPublish() {
        Listing listing = Listing.create(SELLER, "Tour package", null, WINDOW, 5);
        repository.save(listing);

        listing.publish();
        repository.save(listing);

        Optional<Listing> found = repository.findById(listing.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(ListingStatus.ACTIVE);
    }

    @Test
    void findAllByStatusReturnsOnlyMatchingListings() {
        Listing draft = Listing.create(SELLER, "Draft listing", null, WINDOW, 1);
        Listing active = Listing.create(SELLER, "Active listing", null, WINDOW, 1);
        repository.save(draft);
        repository.save(active);
        active.publish();
        repository.save(active);

        List<Listing> activeListings = repository.findAllByStatus(ListingStatus.ACTIVE);
        List<Listing> draftListings = repository.findAllByStatus(ListingStatus.DRAFT);

        assertThat(activeListings).hasSize(1);
        assertThat(activeListings.get(0).getTitle()).isEqualTo("Active listing");
        assertThat(draftListings).hasSize(1);
        assertThat(draftListings.get(0).getTitle()).isEqualTo("Draft listing");
    }
}
