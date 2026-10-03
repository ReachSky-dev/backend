package pl.reachsky.backend.auction.adapter.out.persistence;

import org.springframework.stereotype.Component;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.Bid;
import pl.reachsky.backend.auction.domain.BidId;
import pl.reachsky.backend.auction.domain.BidderId;
import pl.reachsky.backend.auction.domain.ProxyBid;
import pl.reachsky.backend.shared.Money;

import java.util.Currency;

@Component
class BidPersistenceMapper {

    BidJpaEntity toEntity(Bid b) {
        BidJpaEntity e = new BidJpaEntity();
        e.id = b.getId().value();
        e.auctionId = b.getAuctionId().value();
        e.bidderId = b.getBidderId().value();
        e.amount = b.getAmount().amountInMinorUnits();
        e.currency = b.getAmount().currency().getCurrencyCode();
        e.sequence = b.getSequence();
        e.placedAt = b.getPlacedAt();
        e.idempotencyKey = b.getIdempotencyKey();
        return e;
    }

    Bid toDomain(BidJpaEntity e) {
        return Bid.reconstitute(
                new BidId(e.id),
                new AuctionId(e.auctionId),
                new BidderId(e.bidderId),
                new Money(e.amount, Currency.getInstance(e.currency)),
                e.sequence,
                e.placedAt,
                e.idempotencyKey);
    }

    ProxyBidJpaEntity toEntity(ProxyBid p) {
        ProxyBidJpaEntity e = new ProxyBidJpaEntity();
        e.id = p.getId();
        e.auctionId = p.getAuctionId().value();
        e.bidderId = p.getBidderId().value();
        e.maxAmount = p.getMaxAmount().amountInMinorUnits();
        e.currency = p.getMaxAmount().currency().getCurrencyCode();
        e.createdAt = p.getCreatedAt();
        return e;
    }

    ProxyBid toDomain(ProxyBidJpaEntity e) {
        return ProxyBid.reconstitute(
                e.id,
                new AuctionId(e.auctionId),
                new BidderId(e.bidderId),
                new Money(e.maxAmount, Currency.getInstance(e.currency)),
                e.createdAt);
    }
}
