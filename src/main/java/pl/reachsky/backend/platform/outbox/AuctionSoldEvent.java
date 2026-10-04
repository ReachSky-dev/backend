package pl.reachsky.backend.platform.outbox;

import java.util.UUID;

/**
 * Spring application event published by OutboxPoller when an AUCTION_SOLD outbox event is processed.
 */
public record AuctionSoldEvent(UUID auctionId, UUID buyerId, long amountInMinorUnits, String currency) {}
