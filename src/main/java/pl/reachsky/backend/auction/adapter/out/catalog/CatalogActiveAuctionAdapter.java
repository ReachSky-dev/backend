package pl.reachsky.backend.auction.adapter.out.catalog;

import org.springframework.stereotype.Component;
import pl.reachsky.backend.auction.application.port.out.AuctionRepository;
import pl.reachsky.backend.catalog.application.port.out.ActiveAuctionPort;

import java.util.UUID;

/**
 * Bridges the catalog module's ActiveAuctionPort to auction persistence.
 * Catalog defines what it needs; this adapter fulfils it without catalog
 * importing any auction domain or infrastructure types.
 */
@Component
class CatalogActiveAuctionAdapter implements ActiveAuctionPort {

    private final AuctionRepository auctionRepository;

    CatalogActiveAuctionAdapter(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }

    @Override
    public boolean hasActiveAuction(UUID listingId) {
        return auctionRepository.existsActiveByListingId(listingId);
    }
}
