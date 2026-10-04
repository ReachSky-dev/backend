package pl.reachsky.backend.catalog.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.reachsky.backend.catalog.domain.ListingStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface ListingSpringDataRepository extends JpaRepository<ListingJpaEntity, UUID> {

    List<ListingJpaEntity> findAllByStatus(ListingStatus status);

    List<ListingJpaEntity> findAllByStatusAndWindowEndsAtLessThanEqual(ListingStatus status, Instant now);
}
