package pl.reachsky.backend.auction.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.auction.application.port.out.AuctionRepository;
import pl.reachsky.backend.auction.domain.Auction;

import java.time.Instant;
import java.util.List;

@Service
public class AuctionLifecycleService {

    private final AuctionRepository auctionRepository;

    AuctionLifecycleService(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
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
}
