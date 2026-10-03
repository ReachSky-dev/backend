package pl.reachsky.backend.auction.domain;

import org.junit.jupiter.api.Test;
import pl.reachsky.backend.shared.Money;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BidDomainTest {

    private static final Instant NOW = Instant.parse("2028-06-01T10:00:00Z");
    private static final Instant STARTS = NOW.minus(1, ChronoUnit.HOURS);
    private static final Instant ENDS = NOW.plus(2, ChronoUnit.HOURS);

    private static final Money START_PRICE = Money.of(10_000, "PLN");
    private static final Money INCREMENT = Money.of(500, "PLN");

    private Auction runningEnglishAuction(UUID sellerId) {
        Auction a = Auction.create(UUID.randomUUID(), sellerId, AuctionType.ENGLISH,
                STARTS, ENDS,
                new EnglishPricing(START_PRICE, INCREMENT),
                Money.of(5_000, "PLN"), null);
        a.schedule();
        a.start(STARTS);
        return a;
    }

    @Test
    void firstBid_atStartPrice_isAccepted() {
        UUID sellerId = UUID.randomUUID();
        Auction a = runningEnglishAuction(sellerId);
        BidderId bidder = new BidderId(UUID.randomUUID());

        Bid bid = a.placeBid(bidder, START_PRICE, NOW, "key-1");

        assertThat(bid.getAmount()).isEqualTo(START_PRICE);
        assertThat(bid.getSequence()).isEqualTo(1L);
        assertThat(a.getCurrentPrice()).isEqualTo(START_PRICE);
        assertThat(a.getBidCount()).isEqualTo(1);
        assertThat(a.getHighestBidderId()).isEqualTo(bidder.value());
    }

    @Test
    void secondBid_mustExceedCurrentPlusMincrement() {
        UUID sellerId = UUID.randomUUID();
        Auction a = runningEnglishAuction(sellerId);
        BidderId bidder1 = new BidderId(UUID.randomUUID());
        BidderId bidder2 = new BidderId(UUID.randomUUID());

        a.placeBid(bidder1, START_PRICE, NOW, "key-1");

        Money required = Money.of(START_PRICE.amountInMinorUnits() + INCREMENT.amountInMinorUnits(), "PLN");
        Bid bid2 = a.placeBid(bidder2, required, NOW, "key-2");

        assertThat(bid2.getSequence()).isEqualTo(2L);
        assertThat(a.getCurrentPrice()).isEqualTo(required);
    }

    @Test
    void bidBelowMinimum_throwsBidTooLow() {
        UUID sellerId = UUID.randomUUID();
        Auction a = runningEnglishAuction(sellerId);
        BidderId bidder = new BidderId(UUID.randomUUID());

        assertThatThrownBy(() -> a.placeBid(bidder, Money.of(9_999, "PLN"), NOW, "key-1"))
                .isInstanceOf(BidTooLow.class);
    }

    @Test
    void sellerCannotBidOnOwnAuction() {
        UUID sellerId = UUID.randomUUID();
        Auction a = runningEnglishAuction(sellerId);

        assertThatThrownBy(() -> a.placeBid(new BidderId(sellerId), START_PRICE, NOW, "key-1"))
                .isInstanceOf(SellerCannotBid.class);
    }

    @Test
    void bidAfterEndTime_throwsAuctionAlreadyEnded() {
        UUID sellerId = UUID.randomUUID();
        Auction a = runningEnglishAuction(sellerId);
        BidderId bidder = new BidderId(UUID.randomUUID());

        assertThatThrownBy(() -> a.placeBid(bidder, START_PRICE, ENDS, "key-1"))
                .isInstanceOf(AuctionAlreadyEnded.class);
    }

    @Test
    void bidOnNonRunningAuction_throwsAuctionNotRunning() {
        Auction a = Auction.create(UUID.randomUUID(), UUID.randomUUID(), AuctionType.ENGLISH,
                STARTS, ENDS,
                new EnglishPricing(START_PRICE, INCREMENT),
                Money.of(5_000, "PLN"), null);
        // Still DRAFT
        assertThatThrownBy(() -> a.placeBid(new BidderId(UUID.randomUUID()), START_PRICE, NOW, "key-1"))
                .isInstanceOf(AuctionNotRunning.class);
    }

    @Test
    void antiSniping_extendsEndsAt_whenBidWithinWindow() {
        UUID sellerId = UUID.randomUUID();
        AntiSnipingPolicy asp = new AntiSnipingPolicy(
                Duration.ofMinutes(5), Duration.ofMinutes(5), 3);
        Instant ends = NOW.plus(3, ChronoUnit.MINUTES); // within 5-min window
        Auction a = Auction.create(UUID.randomUUID(), sellerId, AuctionType.ENGLISH,
                STARTS, ends,
                new EnglishPricing(START_PRICE, INCREMENT),
                Money.of(5_000, "PLN"), asp);
        a.schedule();
        a.start(STARTS);

        Instant bidTime = ends.minus(1, ChronoUnit.MINUTES); // 1 min before end
        a.placeBid(new BidderId(UUID.randomUUID()), START_PRICE, bidTime, "key-1");

        assertThat(a.getEndsAt()).isAfter(ends);
        assertThat(a.getExtensionsUsed()).isEqualTo(1);
    }

    @Test
    void antiSniping_doesNotExtend_afterMaxExtensions() {
        UUID sellerId = UUID.randomUUID();
        AntiSnipingPolicy asp = new AntiSnipingPolicy(
                Duration.ofMinutes(10), Duration.ofMinutes(5), 1);
        Instant ends = NOW.plus(3, ChronoUnit.MINUTES);
        Auction a = Auction.create(UUID.randomUUID(), sellerId, AuctionType.ENGLISH,
                STARTS, ends,
                new EnglishPricing(START_PRICE, INCREMENT),
                Money.of(5_000, "PLN"), asp);
        a.schedule();
        a.start(STARTS);

        Instant bidTime = ends.minus(1, ChronoUnit.MINUTES);
        a.placeBid(new BidderId(UUID.randomUUID()), START_PRICE, bidTime, "key-1");
        Instant endsAfterFirst = a.getEndsAt();

        // Second bid — maxExtensions = 1, so no more extension
        Instant bidTime2 = endsAfterFirst.minus(1, ChronoUnit.MINUTES);
        a.placeBid(new BidderId(UUID.randomUUID()),
                Money.of(START_PRICE.amountInMinorUnits() + INCREMENT.amountInMinorUnits(), "PLN"),
                bidTime2, "key-2");

        assertThat(a.getEndsAt()).isEqualTo(endsAfterFirst);
        assertThat(a.getExtensionsUsed()).isEqualTo(1);
    }

    @Test
    void domainExceptionCode_isScreamingSnakeCase() {
        BidTooLow ex = new BidTooLow(Money.of(1, "PLN"), Money.of(100, "PLN"));
        assertThat(ex.getCode()).isEqualTo("BID_TOO_LOW");

        SellerCannotBid ex2 = new SellerCannotBid(new AuctionId(UUID.randomUUID()));
        assertThat(ex2.getCode()).isEqualTo("SELLER_CANNOT_BID");
    }
}
