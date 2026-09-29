package pl.reachsky.backend.catalog.adapter.out.persistence;

import org.springframework.stereotype.Repository;
import pl.reachsky.backend.catalog.application.port.out.ListingRepository;
import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingId;
import pl.reachsky.backend.catalog.domain.ListingStatus;

import java.util.List;
import java.util.Optional;

@Repository
class JpaListingRepository implements ListingRepository {

    private final ListingSpringDataRepository springRepo;
    private final ListingPersistenceMapper mapper;

    JpaListingRepository(ListingSpringDataRepository springRepo, ListingPersistenceMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(Listing listing) {
        springRepo.save(mapper.toEntity(listing));
    }

    @Override
    public Optional<Listing> findById(ListingId id) {
        return springRepo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Listing> findAllByStatus(ListingStatus status) {
        return springRepo.findAllByStatus(status).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
