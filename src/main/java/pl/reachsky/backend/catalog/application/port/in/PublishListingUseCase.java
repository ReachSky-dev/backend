package pl.reachsky.backend.catalog.application.port.in;

import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingId;

public interface PublishListingUseCase {

    Listing publish(ListingId id);
}
