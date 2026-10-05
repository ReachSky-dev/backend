package pl.reachsky.backend.catalog.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.catalog.application.port.in.FindListingsQuery;
import pl.reachsky.backend.catalog.application.port.out.ListingRepository;
import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingId;
import pl.reachsky.backend.catalog.domain.ListingStatus;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class FindListingsService implements FindListingsQuery {

    private final ListingRepository repository;
    private final Clock clock;

    FindListingsService(ListingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> findByStatus(ListingStatus status) {
        return repository.findAllByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> findAvailable() {
        return repository.findAvailable(clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Listing> findById(ListingId id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> findBySeller(UUID sellerId) {
        return repository.findBySellerId(sellerId);
    }
}
