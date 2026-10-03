package pl.reachsky.backend.auction.domain;

import pl.reachsky.backend.shared.Money;

import java.time.Duration;
import java.time.Instant;

/**
 * Step-function descending price.
 *
 * Price drops by {@code decrement} every {@code step} duration, starting from {@code startPrice},
 * and never goes below {@code floor}.
 *
 * The function is deterministic: for any given instant the price is unambiguous (I5).
 * Within a step the price is constant, eliminating the race condition that a continuous
 * price function would have (price changes mid-write).
 */
public record DutchPricing(Money startPrice, Money decrement, Duration step, Money floor)
        implements PricingPolicy {

    @Override
    public Money initialPrice() {
        return startPrice;
    }

    @Override
    public Money minimumNextBid(Money currentPrice, int bidCount) {
        return currentPrice;
    }

    @Override
    public Money priceAt(Instant now, Auction auction) {
        Instant effectiveNow = now.isAfter(auction.getEndsAt()) ? auction.getEndsAt() : now;

        if (effectiveNow.isBefore(auction.getStartsAt())) {
            return startPrice;
        }

        long elapsedNanos = Duration.between(auction.getStartsAt(), effectiveNow).toNanos();
        long stepNanos = step.toNanos();
        long steps = stepNanos > 0 ? elapsedNanos / stepNanos : 0;

        long rawAmount = startPrice.amountInMinorUnits() - steps * decrement.amountInMinorUnits();
        long finalAmount = Math.max(floor.amountInMinorUnits(), rawAmount);

        return new Money(finalAmount, startPrice.currency());
    }
}
