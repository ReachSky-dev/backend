package pl.reachsky.backend.auction.adapter.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.reachsky.backend.auction.domain.AuctionStatus;
import pl.reachsky.backend.auction.domain.AuctionType;

import java.time.Instant;
import java.util.UUID;

public record AuctionResponse(
        UUID id,
        UUID listingId,
        UUID sellerId,
        @Schema(ref = "#/components/schemas/AuctionType") AuctionType type,
        @Schema(ref = "#/components/schemas/AuctionStatus") AuctionStatus status,
        @Schema(description = "ISO-8601 UTC instant") Instant startsAt,
        @Schema(description = "ISO-8601 UTC instant") Instant endsAt,
        long currentPriceAmount,
        String currentPriceCurrency,
        // Dutch-auction public fields — null for English
        Long decrementAmount,
        String decrementCurrency,
        Long stepSeconds,
        Instant createdAt
) {}
