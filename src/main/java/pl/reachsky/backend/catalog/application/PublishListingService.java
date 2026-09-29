package pl.reachsky.backend.catalog.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.catalog.application.port.in.PublishListingUseCase;
import pl.reachsky.backend.catalog.application.port.out.ListingRepository;
import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingId;

@Service
class PublishListingService implements PublishListingUseCase {

    private final ListingRepository repository;

    PublishListingService(ListingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Listing publish(ListingId id) {
        Listing listing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + id.value()));
        listing.publish();
        repository.save(listing);
        return listing;
    }
}
