package pl.reachsky.backend.auction.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bids")
class BidJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    UUID id;

    @Column(name = "auction_id", nullable = false, columnDefinition = "uuid")
    UUID auctionId;

    @Column(name = "bidder_id", nullable = false, columnDefinition = "uuid")
    UUID bidderId;

    @Column(nullable = false)
    long amount;

    @Column(nullable = false, length = 3)
    String currency;

    @Column(nullable = false)
    long sequence;

    @Column(name = "placed_at", nullable = false, columnDefinition = "timestamptz")
    Instant placedAt;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    String idempotencyKey;

    BidJpaEntity() {}
}
