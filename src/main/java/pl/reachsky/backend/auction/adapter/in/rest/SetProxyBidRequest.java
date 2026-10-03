package pl.reachsky.backend.auction.adapter.in.rest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SetProxyBidRequest(
        @Min(1) long maxAmountInMinorUnits,
        @NotBlank String currency
) {}
