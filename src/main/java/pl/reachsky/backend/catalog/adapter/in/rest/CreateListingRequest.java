package pl.reachsky.backend.catalog.adapter.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateListingRequest(
        @NotBlank String title,
        String description,
        @NotNull @Schema(description = "ISO-8601 UTC instant, e.g. 2027-09-01T14:00:00Z") Instant windowStart,
        @NotNull @Schema(description = "ISO-8601 UTC instant, e.g. 2027-09-03T10:00:00Z") Instant windowEnd,
        @Min(1) int capacity
) {}
