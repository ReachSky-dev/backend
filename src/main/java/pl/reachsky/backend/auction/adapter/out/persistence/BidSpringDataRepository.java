package pl.reachsky.backend.auction.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface BidSpringDataRepository extends JpaRepository<BidJpaEntity, UUID> {

    Optional<BidJpaEntity> findByIdempotencyKey(String idempotencyKey);

    List<BidJpaEntity> findAllByAuctionIdOrderBySequenceAsc(UUID auctionId);
}
