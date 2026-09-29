package pl.reachsky.backend.catalog.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.catalog.application.port.in.CreateListingCommand;
import pl.reachsky.backend.catalog.application.port.in.CreateListingUseCase;
import pl.reachsky.backend.catalog.application.port.out.ListingRepository;
import pl.reachsky.backend.catalog.domain.Listing;

@Service
class CreateListingService implements CreateListingUseCase {

    private final ListingRepository repository;

    CreateListingService(ListingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Listing create(CreateListingCommand cmd) {
        Listing listing = Listing.create(
                cmd.sellerId(), cmd.title(), cmd.description(), cmd.window(), cmd.capacity());
        repository.save(listing);
        return listing;
    }
}
