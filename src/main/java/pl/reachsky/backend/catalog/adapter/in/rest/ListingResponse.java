package pl.reachsky.backend.catalog.adapter.in.rest;

import java.time.Instant;
import java.util.UUID;

public record ListingResponse(
        UUID id,
        UUID sellerId,
        String title,
        String description,
        Instant windowStart,
        Instant windowEnd,
        int capacity,
        String status,
        Instant createdAt
) {}
