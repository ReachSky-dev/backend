package pl.reachsky.backend.identity.adapter.in.rest;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        Set<String> roles,
        Instant createdAt
) {}
