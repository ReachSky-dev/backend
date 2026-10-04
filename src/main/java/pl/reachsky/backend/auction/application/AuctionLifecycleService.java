package pl.reachsky.backend.auction.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.auction.application.port.in.SettleAuctionUseCase;
import pl.reachsky.backend.auction.application.port.out.AuctionRepository;
import pl.reachsky.backend.auction.domain.Auction;

import java.time.Instant;
import java.util.List;

@Service
public class AuctionLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(AuctionLifecycleService.class);

    private final AuctionRepository auctionRepository;
    private final SettleAuctionUseCase settleAuction;

    AuctionLifecycleService(AuctionRepository auctionRepository, SettleAuctionUseCase settleAuction) {
        this.auctionRepository = auctionRepository;
        this.settleAuction = settleAuction;
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
