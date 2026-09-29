package pl.reachsky.backend.catalog.application.port.in;

import pl.reachsky.backend.catalog.domain.Listing;

public interface CreateListingUseCase {

    Listing create(CreateListingCommand command);
}
