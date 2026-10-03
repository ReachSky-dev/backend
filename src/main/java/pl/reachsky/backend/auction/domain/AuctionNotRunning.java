package pl.reachsky.backend.auction.domain;

import pl.reachsky.backend.shared.DomainException;

public class AuctionNotRunning extends DomainException {

    public AuctionNotRunning(AuctionId id) {
        super("Auction " + id.value() + " is not RUNNING");
    }
}
