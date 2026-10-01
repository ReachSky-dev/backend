package pl.reachsky.backend.auction.domain;

import pl.reachsky.backend.shared.Money;

import java.time.Instant;

public record EnglishPricing(Money startPrice, Money minIncrement) implements PricingPolicy {

    @Override
    public Money priceAt(Instant now, Auction auction) {
        // Current price tracks highest bid — bids come in the next phase.
        // Before any bid, price equals the starting price.
        return startPrice;
    }
}
