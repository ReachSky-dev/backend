package pl.reachsky.backend.auction.domain;

import pl.reachsky.backend.shared.DomainException;

public class SellerCannotBid extends DomainException {

    public SellerCannotBid(AuctionId id) {
        super("Seller cannot place a bid on their own auction " + id.value());
    }
}
