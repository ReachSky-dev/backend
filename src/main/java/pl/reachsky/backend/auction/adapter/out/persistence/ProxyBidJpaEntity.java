package pl.reachsky.backend.auction.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "proxy_bids")
class ProxyBidJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    UUID id;

    @Column(name = "auction_id", nullable = false, columnDefinition = "uuid")
    UUID auctionId;

    @Column(name = "bidder_id", nullable = false, columnDefinition = "uuid")
    UUID bidderId;

    @Column(name = "max_amount", nullable = false)
    long maxAmount;

    @Column(nullable = false, length = 3)
    String currency;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    ProxyBidJpaEntity() {}
}
