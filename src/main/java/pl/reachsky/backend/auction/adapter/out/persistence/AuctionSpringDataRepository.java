package pl.reachsky.backend.auction.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import pl.reachsky.backend.auction.domain.AuctionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AuctionSpringDataRepository extends JpaRepository<AuctionJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AuctionJpaEntity a WHERE a.id = :id")
    Optional<AuctionJpaEntity> findByIdWithLock(UUID id);

    List<AuctionJpaEntity> findAllByStatus(AuctionStatus status);

    List<AuctionJpaEntity> findAllByListingId(UUID listingId);

    List<AuctionJpaEntity> findAllBySellerId(UUID sellerId);

    List<AuctionJpaEntity> findAllByStatusAndStartsAtLessThanEqual(AuctionStatus status, Instant now);

    List<AuctionJpaEntity> findAllByStatusAndEndsAtLessThanEqual(AuctionStatus status, Instant now);
}
