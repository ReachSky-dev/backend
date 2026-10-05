package pl.reachsky.backend.catalog.application.port.out;

import java.util.UUID;

/**
 * Driven port: answers whether a listing has a live auction (SCHEDULED or RUNNING).
 * Implemented by the auction module so that the catalog module does not depend
 * on auction's domain or persistence classes.
 */
public interface ActiveAuctionPort {

    boolean hasActiveAuction(UUID listingId);
}
