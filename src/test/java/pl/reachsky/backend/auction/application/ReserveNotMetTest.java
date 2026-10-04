package pl.reachsky.backend.auction.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.reachsky.backend.AbstractIntegrationTest;
import pl.reachsky.backend.auction.application.port.in.SettleAuctionUseCase;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.SettlementResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * When an auction ends without bids above the reserve, status must be RESERVE_NOT_MET,
 * no order is created, and the outbox event has the correct type.
 */
@SpringBootTest
class ReserveNotMetTest extends AbstractIntegrationTest {

    @Autowired
    SettleAuctionUseCase settleAuction;

    @Autowired
    JdbcTemplate jdbcTemplate;

    UUID auctionId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE outbox_events");
        jdbcTemplate.execute("TRUNCATE TABLE orders");
        jdbcTemplate.execute("TRUNCATE TABLE bids CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE proxy_bids");
        jdbcTemplate.execute("TRUNCATE TABLE auctions CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE listings CASCADE");

        UUID sellerId = UUID.randomUUID();
        UUID listingId = insertListing(sellerId);
        // Auction with reserve 20 000 PLN, ended with no bids
        auctionId = insertEndedAuctionWithHighReserve(listingId, sellerId);
    }

    @Test
    void auctionWithNoBids_settlesAsReserveNotMet() {
        SettlementResult result = settleAuction.settle(new AuctionId(auctionId));

        assertThat(result).isInstanceOf(SettlementResult.ReserveNotMet.class);
        assertThat(auctionStatus(auctionId)).isEqualTo("RESERVE_NOT_MET");
    }

    @Test
    void reserveNotMet_outboxEventWritten_noOrderCreated() {
        settleAuction.settle(new AuctionId(auctionId));

        int outboxCount = countOutboxEvents(auctionId.toString());
        assertThat(outboxCount).as("outbox event written").isEqualTo(1);

        String eventType = jdbcTemplate.queryForObject(
                "SELECT event_type FROM outbox_events WHERE aggregate_id = ?",
                String.class, auctionId.toString());
        assertThat(eventType).isEqualTo("AUCTION_RESERVE_NOT_MET");
    }

    @Test
    void bidBelowReserve_settlesAsReserveNotMet() {
        // Insert bid of 3000 PLN, which is below reserve of 20000 PLN
        insertBid(auctionId, UUID.randomUUID(), 3_000, 1);

        SettlementResult result = settleAuction.settle(new AuctionId(auctionId));

        assertThat(result).isInstanceOf(SettlementResult.ReserveNotMet.class);
        assertThat(auctionStatus(auctionId)).isEqualTo("RESERVE_NOT_MET");
    }

    // -------------------------------------------------------------------------

    private String auctionStatus(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM auctions WHERE id = ?::uuid", String.class, id.toString());
    }

    private int countOutboxEvents(String aggregateId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE aggregate_id = ?",
                Integer.class, aggregateId);
        return count != null ? count : 0;
    }

    private UUID insertListing(UUID sellerId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO listings (id, seller_id, title, window_starts_at, window_ends_at, capacity, status, created_at)
                VALUES (?, ?, 'Reserve test listing', now(), now() + interval '7 day', 1, 'ACTIVE', now())
                """, id, sellerId);
        return id;
    }

    private UUID insertEndedAuctionWithHighReserve(UUID listingId, UUID sellerId) {
        UUID id = UUID.randomUUID();
        Instant starts = Instant.now().minus(3, ChronoUnit.HOURS);
        Instant ends = Instant.now().minus(1, ChronoUnit.HOURS);
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
                    1000, 'PLN',
                    500, 'PLN',
                    20000, 'PLN',
                    1000, 'PLN',
                    0, 0, now())
                """, id, listingId, sellerId,
                java.sql.Timestamp.from(starts), java.sql.Timestamp.from(ends));
        return id;
    }

    private void insertBid(UUID auctionId, UUID bidderId, long amount, int sequence) {
        jdbcTemplate.update(
                "UPDATE auctions SET current_price_amount = ?, bid_count = ?, highest_bidder_id = ?::uuid WHERE id = ?::uuid",
                amount, sequence, bidderId.toString(), auctionId.toString());
        UUID bidId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO bids (id, auction_id, bidder_id, amount, currency, sequence, placed_at, idempotency_key)
                VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'PLN', ?, now(), ?)
                """, bidId, auctionId, bidderId, amount, sequence, "reserve-test-" + sequence);
    }
}
