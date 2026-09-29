package pl.reachsky.backend.catalog.application.port.in;

import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingStatus;

import java.util.List;

public interface FindListingsQuery {

    List<Listing> findByStatus(ListingStatus status);
}
