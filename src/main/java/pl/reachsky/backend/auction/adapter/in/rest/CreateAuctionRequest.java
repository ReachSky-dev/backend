package pl.reachsky.backend.auction.adapter.in.rest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pl.reachsky.backend.auction.domain.AuctionType;

import java.time.Instant;
import java.util.UUID;

public record CreateAuctionRequest(
        @NotNull UUID listingId,
        @NotNull AuctionType type,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @NotNull @Min(1) Long reservePriceAmount,
        @NotBlank String currency,
        @NotNull @Min(1) Long startPriceAmount,
        // English auction
        Long minIncrementAmount,
        // Dutch auction
        Long decrementAmount,
        Long stepSeconds,
        Long floorAmount
) {}
