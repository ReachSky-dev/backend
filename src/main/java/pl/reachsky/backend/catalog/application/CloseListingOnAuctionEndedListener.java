package pl.reachsky.backend.catalog.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.catalog.application.port.out.ListingRepository;
import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingId;
import pl.reachsky.backend.platform.outbox.AuctionReserveNotMetEvent;
import pl.reachsky.backend.platform.outbox.AuctionSoldEvent;

/**
 * Closes or marks a listing as SOLD when its auction is settled.
 *
 * Auction SOLD         → listing SOLD   (resource successfully transferred)
 * Auction RESERVE_NOT_MET → listing CLOSED (auction ended without a buyer)
 *
 * Idempotent: if the listing is already in a terminal state the transition
 * will throw IllegalListingTransition, which is caught and logged so that
 * a duplicate outbox event does not fail the poller.
 */
@Component
public class CloseListingOnAuctionEndedListener {

    private static final Logger log = LoggerFactory.getLogger(CloseListingOnAuctionEndedListener.class);

    private final ListingRepository listingRepository;

    CloseListingOnAuctionEndedListener(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    @EventListener
    @Transactional
    public void onAuctionSold(AuctionSoldEvent event) {
        updateListing(new ListingId(event.listingId()), listing -> listing.sell());
    }

    @EventListener
    @Transactional
    public void onAuctionReserveNotMet(AuctionReserveNotMetEvent event) {
        updateListing(new ListingId(event.listingId()), listing -> listing.close());
    }

    private void updateListing(ListingId id, java.util.function.Consumer<Listing> transition) {
        listingRepository.findById(id).ifPresentOrElse(listing -> {
            try {
                transition.accept(listing);
                listingRepository.save(listing);
            } catch (Exception e) {
                log.warn("Could not transition listing {} — already in terminal state: {}", id.value(), e.getMessage());
            }
        }, () -> log.warn("Listing {} not found during auction settlement", id.value()));
    }
}
