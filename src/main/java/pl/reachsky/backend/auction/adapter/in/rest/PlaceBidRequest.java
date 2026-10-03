package pl.reachsky.backend.auction.adapter.in.rest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PlaceBidRequest(
        @Min(1) long amountInMinorUnits,
        @NotBlank String currency
) {}
