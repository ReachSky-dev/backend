package pl.reachsky.backend.auction.domain;

import pl.reachsky.backend.shared.DomainException;

import java.time.Instant;

public class AuctionAlreadyEnded extends DomainException {

    public AuctionAlreadyEnded(AuctionId id, Instant endsAt, Instant now) {
        super("Auction " + id.value() + " ended at " + endsAt + " (now=" + now + ")");
    }
}
