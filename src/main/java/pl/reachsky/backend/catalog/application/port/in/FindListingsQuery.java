package pl.reachsky.backend.catalog.application.port.in;

import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingId;
import pl.reachsky.backend.catalog.domain.ListingStatus;

import java.util.List;
import java.util.Optional;

public interface FindListingsQuery {

    List<Listing> findByStatus(ListingStatus status);

    List<Listing> findAvailable();

    Optional<Listing> findById(ListingId id);
}
