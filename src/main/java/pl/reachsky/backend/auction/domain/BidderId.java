package pl.reachsky.backend.auction.domain;

import java.util.Objects;
import java.util.UUID;

public record BidderId(UUID value) {

    public BidderId {
        Objects.requireNonNull(value, "BidderId value must not be null");
    }
}
