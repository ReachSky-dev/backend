package pl.reachsky.backend.catalog.adapter.in.rest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateListingRequest(
        @NotBlank String title,
        String description,
        @NotNull Instant windowStart,
        @NotNull Instant windowEnd,
        @Min(1) int capacity
) {}
