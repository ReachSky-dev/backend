package pl.reachsky.backend.auction.domain;

import pl.reachsky.backend.shared.DomainException;

public final class IllegalAuctionTransition extends DomainException {

    public IllegalAuctionTransition(AuctionStatus from, String action) {
        super("Cannot perform '" + action + "' on auction in state " + from);
    }
}
