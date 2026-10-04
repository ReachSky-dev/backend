package pl.reachsky.backend.auction.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.AuctionStatus;
import pl.reachsky.backend.auction.domain.AuctionType;
import pl.reachsky.backend.auction.domain.DutchPricing;
import pl.reachsky.backend.auction.domain.EnglishPricing;
import pl.reachsky.backend.shared.Money;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * I10: the hidden reserve price and Dutch floor must never appear in any API response.
 */
class AuctionReserveNotLeakedTest {

    // Distinctive values unlikely to appear by coincidence in other fields
    private static final long RESERVE_AMOUNT = 99_999_777L;
    private static final long FLOOR_AMOUNT   = 88_888_666L;

    private ObjectMapper mapper;
    private AuctionWebMapper webMapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        webMapper = new AuctionWebMapper();
    }

    @Test
    void englishAuction_reservePriceNotInJson() throws Exception {
        Instant starts = Instant.now().plus(1, ChronoUnit.HOURS);
        EnglishPricing ep = new EnglishPricing(Money.of(20_000, "PLN"), Money.of(500, "PLN"));
        Auction auction = Auction.reconstitute(
                new AuctionId(UUID.randomUUID()), UUID.randomUUID(), UUID.randomUUID(),
                AuctionType.ENGLISH, AuctionStatus.SCHEDULED,
                starts, starts.plus(24, ChronoUnit.HOURS),
                ep, Money.of(RESERVE_AMOUNT, "PLN"), null, Instant.now(),
                ep.initialPrice(), null, 0, 0, null);

        String json = mapper.writeValueAsString(webMapper.toResponse(auction));

        assertThat(json).doesNotContainIgnoringCase("reserve");
        assertThat(json).doesNotContain(String.valueOf(RESERVE_AMOUNT));
    }

    @Test
    void dutchAuction_reserveAndFloorNotInJson() throws Exception {
        Instant starts = Instant.now().plus(1, ChronoUnit.HOURS);
        DutchPricing dp = new DutchPricing(
                Money.of(50_000, "PLN"),
                Money.of(1_000, "PLN"),
                Duration.ofHours(1),
                Money.of(FLOOR_AMOUNT, "PLN"));
        Auction auction = Auction.reconstitute(
                new AuctionId(UUID.randomUUID()), UUID.randomUUID(), UUID.randomUUID(),
                AuctionType.DUTCH, AuctionStatus.SCHEDULED,
                starts, starts.plus(24, ChronoUnit.HOURS),
                dp, Money.of(RESERVE_AMOUNT, "PLN"), null, Instant.now(),
                dp.initialPrice(), null, 0, 0, null);

        String json = mapper.writeValueAsString(webMapper.toResponse(auction));

        assertThat(json).doesNotContainIgnoringCase("reserve");
        assertThat(json).doesNotContainIgnoringCase("floor");
        assertThat(json).doesNotContain(String.valueOf(RESERVE_AMOUNT));
        assertThat(json).doesNotContain(String.valueOf(FLOOR_AMOUNT));
    }
}
