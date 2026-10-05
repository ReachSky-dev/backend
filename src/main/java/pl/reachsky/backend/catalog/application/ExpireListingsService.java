package pl.reachsky.backend.catalog.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.catalog.application.port.out.ActiveAuctionPort;
import pl.reachsky.backend.catalog.application.port.out.ListingRepository;
import pl.reachsky.backend.catalog.domain.Listing;

import java.time.Instant;
import java.util.List;

/**
 * Transitions ACTIVE listings whose resource window has ended to EXPIRED.
 * Listings covered by a live auction (SCHEDULED or RUNNING) are skipped —
 * they will be expired after the auction settles.
 * Idempotent: the repository query only returns ACTIVE listings, so already-EXPIRED
 * ones are never fetched again.
 */
@Service
public class ExpireListingsService {

    private static final Logger log = LoggerFactory.getLogger(ExpireListingsService.class);

    private final ListingRepository listingRepository;
    private final ActiveAuctionPort activeAuctionPort;

    ExpireListingsService(ListingRepository listingRepository, ActiveAuctionPort activeAuctionPort) {
        this.listingRepository = listingRepository;
        this.activeAuctionPort = activeAuctionPort;
    }

    @Transactional
    public void expireDueListings(Instant now) {
        List<Listing> candidates = listingRepository.findActiveExpired(now);
        for (Listing listing : candidates) {
            if (activeAuctionPort.hasActiveAuction(listing.getId().value())) {
                log.debug("Skipping expiry of listing {} — active auction in progress", listing.getId().value());
                continue;
            }
            listing.expire();
            listingRepository.save(listing);
            log.debug("Expired listing {}", listing.getId().value());
        }
    }
}
