package pl.reachsky.backend.catalog.application.port.in;

import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingId;

public interface RenewListingUseCase {

    Listing renew(ListingId id);
}
