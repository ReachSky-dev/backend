package pl.reachsky.backend.auction.domain;

import pl.reachsky.backend.shared.Ids;
import pl.reachsky.backend.shared.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Proxy (automatic) bid — bidder declares a maximum they are willing to pay.
 * One active proxy per (auction, bidder) pair; upserted on each call to SetProxyBidUseCase.
 */
public final class ProxyBid {

    private final UUID id;
    private final AuctionId auctionId;
    private final BidderId bidderId;
    private final Money maxAmount;
    private final Instant createdAt;

    private ProxyBid(UUID id, AuctionId auctionId, BidderId bidderId, Money maxAmount, Instant createdAt) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxAmount = maxAmount;
        this.createdAt = createdAt;
    }

    public static ProxyBid create(AuctionId auctionId, BidderId bidderId, Money maxAmount, Instant now) {
        return new ProxyBid(Ids.next(), auctionId, bidderId, maxAmount, now);
    }

    public static ProxyBid reconstitute(UUID id, AuctionId auctionId, BidderId bidderId,
                                         Money maxAmount, Instant createdAt) {
        return new ProxyBid(id, auctionId, bidderId, maxAmount, createdAt);
    }

    public UUID getId() { return id; }
    public AuctionId getAuctionId() { return auctionId; }
    public BidderId getBidderId() { return bidderId; }
    public Money getMaxAmount() { return maxAmount; }
    public Instant getCreatedAt() { return createdAt; }
}
