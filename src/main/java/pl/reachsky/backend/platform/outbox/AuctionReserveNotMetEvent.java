package pl.reachsky.backend.platform.outbox;

import java.util.UUID;

/**
 * Spring application event published by OutboxPoller when an AUCTION_RESERVE_NOT_MET outbox event is processed.
 */
public record AuctionReserveNotMetEvent(UUID auctionId, UUID listingId) {}
