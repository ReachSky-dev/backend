package pl.reachsky.backend.auction.domain;

import pl.reachsky.backend.shared.Money;

import java.time.Instant;

public sealed interface PricingPolicy permits EnglishPricing, DutchPricing {

    Money priceAt(Instant now, Auction auction);

    /** Price for the very first bid (before any bids have been placed). */
    Money initialPrice();

    /** Minimum amount accepted for the next bid given current state. */
    Money minimumNextBid(Money currentPrice, int bidCount);
}
