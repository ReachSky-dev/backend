package pl.reachsky.backend.auction.domain;

import java.util.Objects;
import java.util.UUID;

public record AuctionId(UUID value) {

    public AuctionId {
        Objects.requireNonNull(value, "AuctionId value must not be null");
    }
}
