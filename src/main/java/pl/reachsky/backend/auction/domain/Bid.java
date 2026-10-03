package pl.reachsky.backend.auction.domain;

import pl.reachsky.backend.shared.Ids;
import pl.reachsky.backend.shared.Money;

import java.time.Instant;

/**
 * Append-only bid record. Never modified or deleted after creation.
 * Created exclusively via {@link Auction#placeBid}.
 */
public final class Bid {

    private final BidId id;
    private final AuctionId auctionId;
    private final BidderId bidderId;
    private final Money amount;
    private final long sequence;
    private final Instant placedAt;
    private final String idempotencyKey;

    private Bid(BidId id, AuctionId auctionId, BidderId bidderId, Money amount,
                long sequence, Instant placedAt, String idempotencyKey) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.sequence = sequence;
        this.placedAt = placedAt;
        this.idempotencyKey = idempotencyKey;
    }

    static Bid create(AuctionId auctionId, BidderId bidderId, Money amount,
                      long sequence, Instant placedAt, String idempotencyKey) {
        return new Bid(new BidId(Ids.next()), auctionId, bidderId, amount,
                sequence, placedAt, idempotencyKey);
    }

    public static Bid reconstitute(BidId id, AuctionId auctionId, BidderId bidderId, Money amount,
                                    long sequence, Instant placedAt, String idempotencyKey) {
        return new Bid(id, auctionId, bidderId, amount, sequence, placedAt, idempotencyKey);
    }

    public BidId getId() { return id; }
    public AuctionId getAuctionId() { return auctionId; }
    public BidderId getBidderId() { return bidderId; }
    public Money getAmount() { return amount; }
    public long getSequence() { return sequence; }
    public Instant getPlacedAt() { return placedAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
