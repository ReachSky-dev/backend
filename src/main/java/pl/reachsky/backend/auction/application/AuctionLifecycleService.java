package pl.reachsky.backend.auction.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.auction.application.port.in.SettleAuctionUseCase;
import pl.reachsky.backend.auction.application.port.out.AuctionRepository;
import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.AuctionStatus;
import pl.reachsky.backend.platform.outbox.AuctionReserveNotMetEvent;
import pl.reachsky.backend.platform.outbox.AuctionSoldEvent;

import java.time.Instant;
import java.util.List;

@Service
public class AuctionLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(AuctionLifecycleService.class);

    private final AuctionRepository auctionRepository;
    private final SettleAuctionUseCase settleAuction;
    private final ApplicationEventPublisher eventPublisher;

    AuctionLifecycleService(AuctionRepository auctionRepository,
                             SettleAuctionUseCase settleAuction,
                             ApplicationEventPublisher eventPublisher) {
        this.auctionRepository = auctionRepository;
        this.settleAuction = settleAuction;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Transitions all SCHEDULED auctions whose startsAt <= now to RUNNING.
     * Idempotent: already-RUNNING auctions are not touched.
     */
    @Transactional
    public void startDueAuctions(Instant now) {
        List<Auction> due = auctionRepository.findDueToStart(now);
        for (Auction auction : due) {
            auction.start(now);
            auctionRepository.save(auction);
        }
    }

    /**
     * Re-publishes ApplicationEvents for all already-settled auctions so that
     * listeners (CloseListingOnAuctionEndedListener, CreateOrderOnAuctionSoldListener)
     * can catch up after a restart or a deployment that added new listeners.
     *
     * Both listeners are idempotent, so duplicate events are safe.
     */
    public void reconcileListingStatuses() {
        List<Auction> ended = auctionRepository.findEnded();
        for (Auction auction : ended) {
            try {
                if (auction.getStatus() == AuctionStatus.SOLD) {
                    eventPublisher.publishEvent(new AuctionSoldEvent(
                            auction.getId().value(),
                            auction.getListingId(),
                            auction.getWinnerId(),
                            auction.getCurrentPrice().amountInMinorUnits(),
                            auction.getCurrentPrice().currency().getCurrencyCode()));
                } else {
                    eventPublisher.publishEvent(new AuctionReserveNotMetEvent(
                            auction.getId().value(),
                            auction.getListingId()));
                }
            } catch (Exception e) {
                log.warn("Reconciliation failed for auction {}: {}", auction.getId().value(), e.getMessage());
            }
        }
    }

    /**
     * Settles all RUNNING auctions whose endsAt <= now.
     * Each auction is settled in its own transaction (via SettleAuctionUseCase).
     * Idempotent: already-settled auctions are silently skipped.
     */
    public void settleDueAuctions(Instant now) {
        List<Auction> due = auctionRepository.findDueToEnd(now);
        for (Auction auction : due) {
            try {
                settleAuction.settle(auction.getId());
            } catch (Exception e) {
                log.warn("Failed to settle auction {}: {}", auction.getId().value(), e.getMessage());
            }
        }
    }
}
