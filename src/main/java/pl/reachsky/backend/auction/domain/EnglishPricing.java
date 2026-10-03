package pl.reachsky.backend.auction.domain;

import pl.reachsky.backend.shared.Money;

import java.time.Instant;

public record EnglishPricing(Money startPrice, Money minIncrement) implements PricingPolicy {

    @Override
    public Money priceAt(Instant now, Auction auction) {
        return auction.getCurrentPrice();
    }

    @Override
    public Money initialPrice() {
        return startPrice;
    }

    @Override
    public Money minimumNextBid(Money currentPrice, int bidCount) {
        return bidCount == 0 ? startPrice : currentPrice.add(minIncrement);
    }
}
