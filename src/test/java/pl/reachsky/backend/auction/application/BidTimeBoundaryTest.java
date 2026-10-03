package pl.reachsky.backend.auction.application;

import org.junit.jupiter.api.Test;
import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.AuctionAlreadyEnded;
import pl.reachsky.backend.auction.domain.AuctionType;
import pl.reachsky.backend.auction.domain.Bid;
import pl.reachsky.backend.auction.domain.BidderId;
import pl.reachsky.backend.auction.domain.EnglishPricing;
import pl.reachsky.backend.shared.Money;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies invariant I2: auction clock boundary is 1 ms precise.
 * Server clock (injected via Clock) is the only authority for bid time.
 */
class BidTimeBoundaryTest {

    private static final Instant STARTS = Instant.parse("2028-06-01T09:00:00Z");
    private static final Instant ENDS   = Instant.parse("2028-06-01T12:00:00Z");

    private Auction running() {
        Auction a = Auction.create(UUID.randomUUID(), UUID.randomUUID(), AuctionType.ENGLISH,
                STARTS, ENDS,
                new EnglishPricing(Money.of(10_000, "PLN"), Money.of(500, "PLN")),
                Money.of(5_000, "PLN"), null);
        a.schedule();
        a.start(STARTS);
        return a;
    }

    @Test
    void bidAtEndsAtMinusOneMs_isAccepted() {
        Auction a = running();
        Instant justBefore = ENDS.minus(1, ChronoUnit.MILLIS);
        Bid bid = a.placeBid(new BidderId(UUID.randomUUID()), Money.of(10_000, "PLN"), justBefore, "k1");
        assertThat(bid).isNotNull();
    }

    @Test
    void bidAtExactlyEndsAt_isRejected() {
        Auction a = running();
        assertThatThrownBy(() -> a.placeBid(
                new BidderId(UUID.randomUUID()), Money.of(10_000, "PLN"), ENDS, "k2"))
                .isInstanceOf(AuctionAlreadyEnded.class);
    }

    @Test
    void bidAtEndsAtPlusOneMs_isRejected() {
        Auction a = running();
        Instant after = ENDS.plus(1, ChronoUnit.MILLIS);
        assertThatThrownBy(() -> a.placeBid(
                new BidderId(UUID.randomUUID()), Money.of(10_000, "PLN"), after, "k3"))
                .isInstanceOf(AuctionAlreadyEnded.class);
    }
}
