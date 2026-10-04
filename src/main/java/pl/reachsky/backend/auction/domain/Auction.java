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
    private Instant endsAt;                // mutable: anti-sniping can extend
    private final PricingPolicy pricingPolicy;
    private final Money reservePrice;
    private final AntiSnipingPolicy antiSnipingPolicy;
    private final Instant createdAt;

    // Bid state — updated by placeBid
    private Money currentPrice;
    private UUID highestBidderId;
    private int bidCount;
    private int extensionsUsed;

    // Settlement state — set by settle()
    private UUID winnerId;

    private Auction(AuctionId id, UUID listingId, UUID sellerId, AuctionType type,
                    AuctionStatus status, Instant startsAt, Instant endsAt,
                    PricingPolicy pricingPolicy, Money reservePrice,
                    AntiSnipingPolicy antiSnipingPolicy, Instant createdAt,
                    Money currentPrice, UUID highestBidderId, int bidCount, int extensionsUsed,
                    UUID winnerId) {
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
        this.currentPrice = currentPrice;
        this.highestBidderId = highestBidderId;
        this.bidCount = bidCount;
        this.extensionsUsed = extensionsUsed;
        this.winnerId = winnerId;
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
                antiSnipingPolicy, Instant.now(),
                pricingPolicy.initialPrice(), null, 0, 0, null);
    }

    public static Auction reconstitute(AuctionId id, UUID listingId, UUID sellerId, AuctionType type,
                                        AuctionStatus status, Instant startsAt, Instant endsAt,
                                        PricingPolicy pricingPolicy, Money reservePrice,
                                        AntiSnipingPolicy antiSnipingPolicy, Instant createdAt,
                                        Money currentPrice, UUID highestBidderId,
                                        int bidCount, int extensionsUsed, UUID winnerId) {
        return new Auction(id, listingId, sellerId, type, status, startsAt, endsAt,
                pricingPolicy, reservePrice, antiSnipingPolicy, createdAt,
                currentPrice, highestBidderId, bidCount, extensionsUsed, winnerId);
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

    /**
     * Places a bid on this English auction.
     * Validates state, enforces minimum increment, applies anti-sniping extension.
     * Returns the new Bid — caller must persist both the Bid and the updated Auction.
     */
    public Bid placeBid(BidderId bidderId, Money amount, Instant now, String idempotencyKey) {
        if (status != AuctionStatus.RUNNING) throw new AuctionNotRunning(id);
        if (!now.isBefore(endsAt)) throw new AuctionAlreadyEnded(id, endsAt, now);
        if (bidderId.value().equals(sellerId)) throw new SellerCannotBid(id);

        Money minRequired = pricingPolicy.minimumNextBid(currentPrice, bidCount);
        if (amount.isLessThan(minRequired)) throw new BidTooLow(amount, minRequired);

        currentPrice = amount;
        highestBidderId = bidderId.value();
        long sequence = ++bidCount;

        if (antiSnipingPolicy != null && extensionsUsed < antiSnipingPolicy.maxExtensions()) {
            Instant threshold = endsAt.minus(antiSnipingPolicy.window());
            if (!now.isBefore(threshold)) {
                endsAt = endsAt.plus(antiSnipingPolicy.extension());
                extensionsUsed++;
            }
        }

        return Bid.create(id, bidderId, amount, sequence, now, idempotencyKey);
    }

    /**
     * Settles a finished auction.
     * Idempotent: returns AlreadySettled if already SOLD or RESERVE_NOT_MET.
     */
    public SettlementResult settle() {
        if (status == AuctionStatus.SOLD || status == AuctionStatus.RESERVE_NOT_MET) {
            return new SettlementResult.AlreadySettled();
        }
        if (status != AuctionStatus.RUNNING) {
            throw new AuctionNotRunning(id);
        }
        boolean reserveMet = bidCount > 0 && !currentPrice.isLessThan(reservePrice);
        if (reserveMet) {
            this.status = AuctionStatus.SOLD;
            this.winnerId = highestBidderId;
            return new SettlementResult.Sold(winnerId, currentPrice);
        } else {
            this.status = AuctionStatus.RESERVE_NOT_MET;
            return new SettlementResult.ReserveNotMet();
        }
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
    public Money getCurrentPrice() { return currentPrice; }
    public UUID getHighestBidderId() { return highestBidderId; }
    public int getBidCount() { return bidCount; }
    public int getExtensionsUsed() { return extensionsUsed; }
    public UUID getWinnerId() { return winnerId; }
}
