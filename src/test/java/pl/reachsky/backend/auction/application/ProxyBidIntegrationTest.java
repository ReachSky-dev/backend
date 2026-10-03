package pl.reachsky.backend.auction.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.reachsky.backend.AbstractIntegrationTest;
import pl.reachsky.backend.auction.application.port.in.BidResult;
import pl.reachsky.backend.auction.application.port.in.FindBidsQuery;
import pl.reachsky.backend.auction.application.port.in.PlaceBidCommand;
import pl.reachsky.backend.auction.application.port.in.PlaceBidUseCase;
import pl.reachsky.backend.auction.application.port.in.SetProxyBidCommand;
import pl.reachsky.backend.auction.application.port.in.SetProxyBidUseCase;
import pl.reachsky.backend.auction.application.port.out.AuctionRepository;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.Bid;
import pl.reachsky.backend.auction.domain.BidderId;
import pl.reachsky.backend.shared.Money;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies proxy (automatic) bid behavior:
 * – proxy holder is auto-countered when their proxy can afford the minimum increment
 * – proxy holder is NOT countered when the incoming bid exceeds their max
 */
@SpringBootTest
class ProxyBidIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    PlaceBidUseCase placeBidUseCase;

    @Autowired
    SetProxyBidUseCase setProxyBidUseCase;

    @Autowired
    FindBidsQuery findBidsQuery;

    @Autowired
    AuctionRepository auctionRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    UUID auctionId;
    UUID sellerId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE bids");
        jdbcTemplate.execute("TRUNCATE TABLE proxy_bids");
        jdbcTemplate.execute("TRUNCATE TABLE auctions CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE listings CASCADE");

        sellerId = UUID.randomUUID();
        UUID listingId = insertListing(sellerId);
        auctionId = insertRunningEnglishAuction(listingId, sellerId);
    }

    @Test
    void proxyHolder_isAutoCounted_whenIncomingBidIsWithinProxyMax() {
        UUID proxyBidder = UUID.randomUUID();
        UUID manualBidder = UUID.randomUUID();

        // proxyBidder sets proxy up to 12 000 PLN
        setProxyBidUseCase.set(new SetProxyBidCommand(
                new AuctionId(auctionId),
                new BidderId(proxyBidder),
                Money.of(12_000, "PLN")));

        // manualBidder places bid at 10 000 (startPrice)
        BidResult result = placeBidUseCase.place(new PlaceBidCommand(
                new AuctionId(auctionId),
                new BidderId(manualBidder),
                Money.of(10_000, "PLN"),
                "manual-bid-1"));

        assertThat(result).isInstanceOf(BidResult.Accepted.class);

        // After manual bid: proxy evaluation fires → proxyBidder auto-bids 10 500 (min increment)
        List<Bid> bids = findBidsQuery.findByAuction(new AuctionId(auctionId));
        assertThat(bids).hasSize(2);
        assertThat(bids.get(1).getBidderId().value()).isEqualTo(proxyBidder);
        assertThat(bids.get(1).getAmount()).isEqualTo(Money.of(10_500, "PLN"));

        var auction = auctionRepository.findById(new AuctionId(auctionId)).orElseThrow();
        assertThat(auction.getHighestBidderId()).isEqualTo(proxyBidder);
    }

    @Test
    void proxyHolder_isNotCountered_whenIncomingBidExceedsProxyMax() {
        UUID proxyBidder = UUID.randomUUID();
        UUID manualBidder = UUID.randomUUID();

        // proxyBidder sets proxy up to 11 000 PLN
        setProxyBidUseCase.set(new SetProxyBidCommand(
                new AuctionId(auctionId),
                new BidderId(proxyBidder),
                Money.of(11_000, "PLN")));

        // manualBidder bids 12 000 — exceeds proxy max
        placeBidUseCase.place(new PlaceBidCommand(
                new AuctionId(auctionId),
                new BidderId(manualBidder),
                Money.of(12_000, "PLN"),
                "manual-bid-2"));

        List<Bid> bids = findBidsQuery.findByAuction(new AuctionId(auctionId));
        assertThat(bids).hasSize(1); // no auto-counter

        var auction = auctionRepository.findById(new AuctionId(auctionId)).orElseThrow();
        assertThat(auction.getHighestBidderId()).isEqualTo(manualBidder);
    }

    // -------------------------------------------------------------------------

    private UUID insertListing(UUID sid) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO listings (id, seller_id, title, window_starts_at, window_ends_at, capacity, status, created_at)
                VALUES (?, ?, 'Test listing', now(), now() + interval '7 day', 1, 'ACTIVE', now())
                """, id, sid);
        return id;
    }

    private UUID insertRunningEnglishAuction(UUID listingId, UUID sid) {
        UUID id = UUID.randomUUID();
        Instant starts = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant ends = Instant.now().plus(2, ChronoUnit.HOURS);
        jdbcTemplate.update("""
                INSERT INTO auctions (id, listing_id, seller_id, type, status,
                    starts_at, ends_at,
                    start_price_amount, start_price_currency,
                    min_increment_amount, min_increment_currency,
                    reserve_price_amount, reserve_price_currency,
                    current_price_amount, current_price_currency,
                    bid_count, extensions_used, created_at)
                VALUES (?, ?, ?, 'ENGLISH', 'RUNNING',
                    ?, ?,
                    10000, 'PLN', 500, 'PLN', 5000, 'PLN',
                    10000, 'PLN', 0, 0, now())
                """, id, listingId, sid,
                java.sql.Timestamp.from(starts), java.sql.Timestamp.from(ends));
        return id;
    }
}
