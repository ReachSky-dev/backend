package pl.reachsky.backend.catalog.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.catalog.application.port.in.RenewListingUseCase;
import pl.reachsky.backend.catalog.application.port.out.ListingRepository;
import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingId;
import pl.reachsky.backend.shared.NotFoundException;

@Service
class RenewListingService implements RenewListingUseCase {

    private final ListingRepository listingRepository;

    RenewListingService(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    @Override
    @Transactional
    public Listing renew(ListingId id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Listing not found: " + id.value()));
        listing.renew();
        listingRepository.save(listing);
        return listing;
    }
}
