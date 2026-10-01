package pl.reachsky.backend.auction.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.reachsky.backend.auction.domain.AuctionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface AuctionSpringDataRepository extends JpaRepository<AuctionJpaEntity, UUID> {

    List<AuctionJpaEntity> findAllByStatus(AuctionStatus status);

    List<AuctionJpaEntity> findAllByListingId(UUID listingId);

    List<AuctionJpaEntity> findAllByStatusAndStartsAtLessThanEqual(AuctionStatus status, Instant now);

    List<AuctionJpaEntity> findAllByStatusAndEndsAtLessThanEqual(AuctionStatus status, Instant now);
}
