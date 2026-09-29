package pl.reachsky.backend.catalog.adapter.in.rest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateListingRequest(
        @NotNull UUID sellerId,
        @NotBlank String title,
        String description,
        @NotNull Instant windowStart,
        @NotNull Instant windowEnd,
        @Min(1) int capacity
) {}
