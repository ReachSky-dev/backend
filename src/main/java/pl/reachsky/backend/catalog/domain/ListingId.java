package pl.reachsky.backend.catalog.domain;

import java.util.UUID;

public record ListingId(UUID value) {

    public ListingId {
        if (value == null) throw new IllegalArgumentException("ListingId value must not be null");
    }
}
