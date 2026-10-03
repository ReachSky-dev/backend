package pl.reachsky.backend.auction.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProxyBidSpringDataRepository extends JpaRepository<ProxyBidJpaEntity, UUID> {

    Optional<ProxyBidJpaEntity> findByAuctionIdAndBidderId(UUID auctionId, UUID bidderId);

    List<ProxyBidJpaEntity> findAllByAuctionId(UUID auctionId);
}
