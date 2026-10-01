package pl.reachsky.backend.auction.domain;

import org.junit.jupiter.api.Test;
import pl.reachsky.backend.shared.Money;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionTest {

    private static final Instant STARTS = Instant.now().plus(1, ChronoUnit.HOURS);
    private static final Instant ENDS   = STARTS.plus(24, ChronoUnit.HOURS);

    private Auction englishDraft() {
        return Auction.create(UUID.randomUUID(), UUID.randomUUID(), AuctionType.ENGLISH,
                STARTS, ENDS,
                new EnglishPricing(Money.of(10_000, "PLN"), Money.of(500, "PLN")),
                Money.of(5_000, "PLN"), null);
    }

    // Allowed transitions

    @Test
    void draftToScheduled_allowed() {
        Auction a = englishDraft();
        a.schedule();
        assertThat(a.getStatus()).isEqualTo(AuctionStatus.SCHEDULED);
    }

    @Test
    void scheduledToRunning_allowed() {
        Auction a = englishDraft();
        a.schedule();
        a.start(Instant.now());
        assertThat(a.getStatus()).isEqualTo(AuctionStatus.RUNNING);
    }

    @Test
    void draftToCancelled_allowed() {
        Auction a = englishDraft();
        a.cancel();
        assertThat(a.getStatus()).isEqualTo(AuctionStatus.CANCELLED);
    }

    @Test
    void scheduledToCancelled_allowed() {
        Auction a = englishDraft();
        a.schedule();
        a.cancel();
        assertThat(a.getStatus()).isEqualTo(AuctionStatus.CANCELLED);
    }

    @Test
    void runningToCancelled_allowed() {
        Auction a = englishDraft();
        a.schedule();
        a.start(Instant.now());
        a.cancel();
        assertThat(a.getStatus()).isEqualTo(AuctionStatus.CANCELLED);
    }

    // Forbidden transitions

    @Test
    void draftCannotStart() {
        Auction a = englishDraft();
        assertThatThrownBy(() -> a.start(Instant.now()))
                .isInstanceOf(IllegalAuctionTransition.class)
                .hasMessageContaining("DRAFT");
    }

    @Test
    void scheduledCannotScheduleAgain() {
        Auction a = englishDraft();
        a.schedule();
        assertThatThrownBy(a::schedule)
                .isInstanceOf(IllegalAuctionTransition.class)
                .hasMessageContaining("SCHEDULED");
    }

    @Test
    void cancelledCannotCancel() {
        Auction a = englishDraft();
        a.cancel();
        assertThatThrownBy(a::cancel)
                .isInstanceOf(IllegalAuctionTransition.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void runningCannotStart() {
        Auction a = englishDraft();
        a.schedule();
        a.start(Instant.now());
        assertThatThrownBy(() -> a.start(Instant.now()))
                .isInstanceOf(IllegalAuctionTransition.class)
                .hasMessageContaining("RUNNING");
    }

    // isRunningAt

    @Test
    void isRunningAt_trueWhenRunningAndWithinWindow() {
        Auction a = englishDraft();
        a.schedule();
        a.start(Instant.now());
        assertThat(a.isRunningAt(STARTS.plus(1, ChronoUnit.MINUTES))).isTrue();
    }

    @Test
    void isRunningAt_falseBeforeWindow() {
        Auction a = englishDraft();
        a.schedule();
        a.start(Instant.now());
        // now < startsAt
        assertThat(a.isRunningAt(STARTS.minus(1, ChronoUnit.MINUTES))).isFalse();
    }

    @Test
    void isRunningAt_falseAfterWindow() {
        Auction a = englishDraft();
        a.schedule();
        a.start(Instant.now());
        assertThat(a.isRunningAt(ENDS.plus(1, ChronoUnit.MINUTES))).isFalse();
    }
}
