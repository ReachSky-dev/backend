package pl.reachsky.backend.auction.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.reachsky.backend.shared.Money;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies invariant I5: Dutch price is deterministic — f(auctionId, t) always returns the same value.
 */
class DutchPricingTest {

    // Fixed epoch for determinism — no Instant.now()
    private static final Instant EPOCH    = Instant.parse("2027-09-01T12:00:00Z");
    private static final Instant STARTS   = EPOCH;
    private static final Instant ENDS     = EPOCH.plus(Duration.ofHours(10));

    private static final Money START_PRICE  = Money.of(10_000, "PLN"); // 100.00 PLN
    private static final Money DECREMENT    = Money.of(1_000,  "PLN"); // 10.00 PLN per step
    private static final Duration STEP      = Duration.ofHours(1);
    private static final Money FLOOR        = Money.of(3_000,  "PLN"); // 30.00 PLN

    private DutchPricing pricing;
    private Auction auction;

    @BeforeEach
    void setUp() {
        pricing = new DutchPricing(START_PRICE, DECREMENT, STEP, FLOOR);
        auction = Auction.reconstitute(
                new AuctionId(UUID.randomUUID()), UUID.randomUUID(), UUID.randomUUID(),
                AuctionType.DUTCH, AuctionStatus.RUNNING,
                STARTS, ENDS, pricing, Money.of(3_000, "PLN"), null, EPOCH,
                pricing.initialPrice(), null, 0, 0, null);
    }

    @Test
    void priceBeforeStart_returnsStartPrice() {
        Money price = pricing.priceAt(STARTS.minus(Duration.ofMinutes(1)), auction);
        assertThat(price.amountInMinorUnits()).isEqualTo(10_000);
    }

    @Test
    void priceAtStart_returnsStartPrice() {
        Money price = pricing.priceAt(STARTS, auction);
        assertThat(price.amountInMinorUnits()).isEqualTo(10_000);
    }

    @Test
    void priceIsConstantWithinStep() {
        // step 0 → price = 10000 for all t in [STARTS, STARTS + 1h)
        Money at1min  = pricing.priceAt(STARTS.plus(Duration.ofMinutes(1)),  auction);
        Money at30min = pricing.priceAt(STARTS.plus(Duration.ofMinutes(30)), auction);
        Money at59min = pricing.priceAt(STARTS.plus(Duration.ofMinutes(59)), auction);

        assertThat(at1min.amountInMinorUnits()).isEqualTo(10_000);
        assertThat(at30min.amountInMinorUnits()).isEqualTo(10_000);
        assertThat(at59min.amountInMinorUnits()).isEqualTo(10_000);
    }

    @Test
    void priceDropsExactlyOnStepBoundary() {
        // At exactly t = STARTS + 1h → step 1 → price = 9000
        Money atStep1 = pricing.priceAt(STARTS.plus(STEP), auction);
        assertThat(atStep1.amountInMinorUnits()).isEqualTo(9_000);

        // At exactly t = STARTS + 2h → step 2 → price = 8000
        Money atStep2 = pricing.priceAt(STARTS.plus(STEP.multipliedBy(2)), auction);
        assertThat(atStep2.amountInMinorUnits()).isEqualTo(8_000);
    }

    @Test
    void priceNeverGoesBelowFloor() {
        // After 7 steps: 10000 - 7*1000 = 3000 = floor
        Money atStep7  = pricing.priceAt(STARTS.plus(STEP.multipliedBy(7)),  auction);
        // After 8 steps: would be 2000 < floor → clamped to 3000
        Money atStep8  = pricing.priceAt(STARTS.plus(STEP.multipliedBy(8)),  auction);
        Money atStep10 = pricing.priceAt(STARTS.plus(STEP.multipliedBy(10)), auction);

        assertThat(atStep7.amountInMinorUnits()).isEqualTo(3_000);
        assertThat(atStep8.amountInMinorUnits()).isEqualTo(3_000);
        assertThat(atStep10.amountInMinorUnits()).isEqualTo(3_000);
    }

    @Test
    void priceAfterEnd_returnsLastStepPrice() {
        // ENDS = STARTS + 10h → step 10 → clamped to floor
        Money afterEnd = pricing.priceAt(ENDS.plus(Duration.ofHours(5)), auction);
        assertThat(afterEnd.amountInMinorUnits()).isEqualTo(3_000);
    }

    @Test
    void determinism_sameMomentSamePrice(org.junit.jupiter.api.TestInfo info) {
        // I5: repeated calls with identical instant must produce identical price
        Instant fixed = STARTS.plus(Duration.ofMinutes(90)); // mid step-1
        Money first  = pricing.priceAt(fixed, auction);
        Money second = pricing.priceAt(fixed, auction);
        Money third  = pricing.priceAt(fixed, auction);

        assertThat(first.amountInMinorUnits()).isEqualTo(second.amountInMinorUnits());
        assertThat(second.amountInMinorUnits()).isEqualTo(third.amountInMinorUnits());
        // At 90 min: step = 1, price = 9000
        assertThat(first.amountInMinorUnits()).isEqualTo(9_000);
    }
}
