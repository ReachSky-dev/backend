package pl.reachsky.backend.auction.domain;

import java.util.Objects;
import java.util.UUID;

public record BidId(UUID value) {

    public BidId {
        Objects.requireNonNull(value, "BidId value must not be null");
    }
}
