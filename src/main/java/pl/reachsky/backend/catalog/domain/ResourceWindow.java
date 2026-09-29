package pl.reachsky.backend.catalog.domain;

import java.time.Instant;

/**
 * Time window during which the resource is provided (e.g. hotel check-in to check-out).
 */
public record ResourceWindow(Instant startsAt, Instant endsAt) {

    public ResourceWindow {
        if (startsAt == null) throw new IllegalArgumentException("startsAt must not be null");
        if (endsAt == null) throw new IllegalArgumentException("endsAt must not be null");
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
    }
}
