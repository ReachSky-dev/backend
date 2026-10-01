package pl.reachsky.backend.auction.domain;

import pl.reachsky.backend.shared.Money;

import java.time.Instant;

public sealed interface PricingPolicy permits EnglishPricing, DutchPricing {

    Money priceAt(Instant now, Auction auction);
}
