package pl.reachsky.backend.auction.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pl.reachsky.backend.auction.domain.AuctionStatus;
import pl.reachsky.backend.auction.domain.AuctionType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auctions")
class AuctionJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    UUID id;

    @Column(name = "listing_id", nullable = false, columnDefinition = "uuid")
    UUID listingId;

    @Column(name = "seller_id", nullable = false, columnDefinition = "uuid")
    UUID sellerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    AuctionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    AuctionStatus status;

    @Column(name = "starts_at", nullable = false, columnDefinition = "timestamptz")
    Instant startsAt;

    @Column(name = "ends_at", nullable = false, columnDefinition = "timestamptz")
    Instant endsAt;

    // PricingPolicy — common
    @Column(name = "start_price_amount", nullable = false)
    long startPriceAmount;

    @Column(name = "start_price_currency", nullable = false, length = 3)
    String startPriceCurrency;

    // EnglishPricing
    @Column(name = "min_increment_amount")
    Long minIncrementAmount;

    @Column(name = "min_increment_currency", length = 3)
    String minIncrementCurrency;

    // DutchPricing
    @Column(name = "decrement_amount")
    Long decrementAmount;

    @Column(name = "decrement_currency", length = 3)
    String decrementCurrency;

    @Column(name = "step_seconds")
    Long stepSeconds;

    @Column(name = "floor_amount")
    Long floorAmount;

    @Column(name = "floor_currency", length = 3)
    String floorCurrency;

    // Reserve price (hidden from API)
    @Column(name = "reserve_price_amount", nullable = false)
    long reservePriceAmount;

    @Column(name = "reserve_price_currency", nullable = false, length = 3)
    String reservePriceCurrency;

    // AntiSnipingPolicy (nullable)
    @Column(name = "anti_sniping_window_s")
    Long antiSnipingWindowS;

    @Column(name = "anti_sniping_ext_s")
    Long antiSnipingExtS;

    @Column(name = "anti_sniping_max_ext")
    Integer antiSnipingMaxExt;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    // Bid state
    @Column(name = "current_price_amount", nullable = false)
    long currentPriceAmount;

    @Column(name = "current_price_currency", nullable = false, length = 3)
    String currentPriceCurrency;

    @Column(name = "highest_bidder_id", columnDefinition = "uuid")
    UUID highestBidderId;

    @Column(name = "bid_count", nullable = false)
    int bidCount;

    @Column(name = "extensions_used", nullable = false)
    int extensionsUsed;

    AuctionJpaEntity() {}
}
