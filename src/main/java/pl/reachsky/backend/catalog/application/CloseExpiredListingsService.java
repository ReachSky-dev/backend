package pl.reachsky.backend.catalog.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.catalog.application.port.out.ListingRepository;
import pl.reachsky.backend.catalog.domain.Listing;

import java.time.Clock;
import java.util.List;

/**
 * Closes ACTIVE listings whose resource window has ended.
 * Called by the scheduler; idempotent — re-closing is guarded by ListingStatus.close().
 */
@Service
public class CloseExpiredListingsService {

    private static final Logger log = LoggerFactory.getLogger(CloseExpiredListingsService.class);

    private final ListingRepository listingRepository;
    private final Clock clock;

    CloseExpiredListingsService(ListingRepository listingRepository, Clock clock) {
        this.listingRepository = listingRepository;
        this.clock = clock;
    }

    @Transactional
    public void closeExpired() {
        List<Listing> expired = listingRepository.findActiveExpired(clock.instant());
        for (Listing listing : expired) {
            listing.close();
            listingRepository.save(listing);
            log.debug("Closed expired listing {}", listing.getId().value());
        }
    }
}
