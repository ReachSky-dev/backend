package pl.reachsky.backend.catalog.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.catalog.application.port.in.FindListingsQuery;
import pl.reachsky.backend.catalog.application.port.out.ListingRepository;
import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingStatus;

import java.util.List;

@Service
class FindListingsService implements FindListingsQuery {

    private final ListingRepository repository;

    FindListingsService(ListingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> findByStatus(ListingStatus status) {
        return repository.findAllByStatus(status);
    }
}
