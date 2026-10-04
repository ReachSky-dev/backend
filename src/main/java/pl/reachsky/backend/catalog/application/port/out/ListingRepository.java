package pl.reachsky.backend.catalog.application.port.out;

import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingId;
import pl.reachsky.backend.catalog.domain.ListingStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ListingRepository {

    void save(Listing listing);

    Optional<Listing> findById(ListingId id);

    List<Listing> findAllByStatus(ListingStatus status);

    List<Listing> findActiveExpired(Instant now);

    List<Listing> findAvailable(Instant now);
}
