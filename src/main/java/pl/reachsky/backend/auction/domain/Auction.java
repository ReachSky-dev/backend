package pl.reachsky.backend.auction.domain;

import pl.reachsky.backend.shared.Ids;
import pl.reachsky.backend.shared.Money;

import java.time.Instant;
import java.util.UUID;

public final class Auction {

    private final AuctionId id;
    private final UUID listingId;
    private final UUID sellerId;
    private final AuctionType type;
    private AuctionStatus status;
    private final Instant startsAt;
    private final Instant endsAt;
    private final PricingPolicy pricingPolicy;
    private final Money reservePrice;
    private final AntiSnipingPolicy antiSnipingPolicy;
    private final Instant createdAt;

    private Auction(AuctionId id, UUID listingId, UUID sellerId, AuctionType type,
                    AuctionStatus status, Instant startsAt, Instant endsAt,
                    PricingPolicy pricingPolicy, Money reservePrice,
                    AntiSnipingPolicy antiSnipingPolicy, Instant createdAt) {
        this.id = id;
        this.listingId = listingId;
        this.sellerId = sellerId;
        this.type = type;
        this.status = status;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.pricingPolicy = pricingPolicy;
        this.reservePrice = reservePrice;
        this.antiSnipingPolicy = antiSnipingPolicy;
        this.createdAt = createdAt;
    }

    public static Auction create(UUID listingId, UUID sellerId, AuctionType type,
                                  Instant startsAt, Instant endsAt,
                                  PricingPolicy pricingPolicy, Money reservePrice,
                                  AntiSnipingPolicy antiSnipingPolicy) {
        if (listingId == null) throw new IllegalArgumentException("listingId must not be null");
        if (sellerId == null) throw new IllegalArgumentException("sellerId must not be null");
        if (startsAt == null || endsAt == null) throw new IllegalArgumentException("startsAt and endsAt must not be null");
        if (!endsAt.isAfter(startsAt)) throw new IllegalArgumentException("endsAt must be after startsAt");
        return new Auction(new AuctionId(Ids.next()), listingId, sellerId, type,
                AuctionStatus.DRAFT, startsAt, endsAt, pricingPolicy, reservePrice,
                antiSnipingPolicy, Instant.now());
    }

    public static Auction reconstitute(AuctionId id, UUID listingId, UUID sellerId, AuctionType type,
                                        AuctionStatus status, Instant startsAt, Instant endsAt,
                                        PricingPolicy pricingPolicy, Money reservePrice,
                                        AntiSnipingPolicy antiSnipingPolicy, Instant createdAt) {
        return new Auction(id, listingId, sellerId, type, status, startsAt, endsAt,
                pricingPolicy, reservePrice, antiSnipingPolicy, createdAt);
    }

    public void schedule() {
        this.status = status.schedule();
    }

    public void start(Instant now) {
        this.status = status.start();
    }

    public void cancel() {
        this.status = status.cancel();
    }

    public Money currentPriceAt(Instant now) {
        return pricingPolicy.priceAt(now, this);
    }

    public boolean isRunningAt(Instant now) {
        return status == AuctionStatus.RUNNING
                && !now.isBefore(startsAt)
                && now.isBefore(endsAt);
    }

    public AuctionId getId() { return id; }
    public UUID getListingId() { return listingId; }
    public UUID getSellerId() { return sellerId; }
    public AuctionType getType() { return type; }
    public AuctionStatus getStatus() { return status; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public PricingPolicy getPricingPolicy() { return pricingPolicy; }
    public Money getReservePrice() { return reservePrice; }
    public AntiSnipingPolicy getAntiSnipingPolicy() { return antiSnipingPolicy; }
    public Instant getCreatedAt() { return createdAt; }
}
