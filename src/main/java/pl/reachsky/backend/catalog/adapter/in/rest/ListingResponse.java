package pl.reachsky.backend.catalog.adapter.in.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.reachsky.backend.catalog.domain.ListingStatus;

import java.time.Instant;
import java.util.UUID;

public record ListingResponse(
        UUID id,
        UUID sellerId,
        String title,
        String description,
        @Schema(description = "ISO-8601 UTC instant") Instant windowStart,
        @Schema(description = "ISO-8601 UTC instant") Instant windowEnd,
        int capacity,
        @Schema(ref = "#/components/schemas/ListingStatus") ListingStatus status,
        Instant createdAt
) {}
