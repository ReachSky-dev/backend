package pl.reachsky.backend.auction.adapter.in.rest;

import java.time.Instant;
import java.util.UUID;

public record BidResponse(
        UUID id,
        UUID auctionId,
        UUID bidderId,
        long amountInMinorUnits,
        String currency,
        long sequence,
        Instant placedAt
) {}
