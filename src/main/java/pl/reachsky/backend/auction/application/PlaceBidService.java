package pl.reachsky.backend.auction.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.auction.application.port.in.BidResult;
import pl.reachsky.backend.auction.application.port.in.PlaceBidCommand;
import pl.reachsky.backend.auction.application.port.in.PlaceBidUseCase;
import pl.reachsky.backend.auction.application.port.out.AuctionRepository;
import pl.reachsky.backend.auction.application.port.out.BidRepository;
import pl.reachsky.backend.auction.application.port.out.ProxyBidRepository;
import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.Bid;
import pl.reachsky.backend.auction.domain.ProxyBid;
import pl.reachsky.backend.shared.Money;
import pl.reachsky.backend.shared.NotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
class PlaceBidService implements PlaceBidUseCase {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ProxyBidRepository proxyBidRepository;
    private final Clock clock;

    PlaceBidService(AuctionRepository auctionRepository, BidRepository bidRepository,
                    ProxyBidRepository proxyBidRepository, Clock clock) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.proxyBidRepository = proxyBidRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BidResult place(PlaceBidCommand cmd) {
        // Step 1 — idempotency: return existing bid if key was already used
        Optional<Bid> existing = bidRepository.findByIdempotencyKey(cmd.idempotencyKey());
        if (existing.isPresent()) return new BidResult.Duplicate(existing.get());

        // Step 2 — load auction with PESSIMISTIC_WRITE lock
        Auction auction = auctionRepository.findByIdForUpdate(cmd.auctionId())
                .orElseThrow(() -> new NotFoundException("Auction not found: " + cmd.auctionId().value()));

        // Step 3 — domain validates and updates state
        Instant now = clock.instant();
        Bid bid = auction.placeBid(cmd.bidderId(), cmd.amount(), now, cmd.idempotencyKey());

        // Step 4 — persist bid
        bidRepository.save(bid);

        // Step 5 — evaluate proxy bids for remaining competitors
        evaluateProxies(auction, now);

        // Step 6 — persist updated auction (currentPrice, endsAt, bidCount, extensionsUsed)
        auctionRepository.save(auction);

        return new BidResult.Accepted(bid);
    }

    /**
     * After a new bid, finds the strongest proxy that can outbid the current winner
     * and places one automatic counter-bid on their behalf.
     * One round per call — sufficient for the common case; nested proxy wars
     * are out of scope for phase 5.
     */
    private void evaluateProxies(Auction auction, Instant now) {
        List<ProxyBid> proxies = proxyBidRepository.findByAuctionId(auction.getId());
        if (proxies.isEmpty()) return;

        Money minRequired = auction.getPricingPolicy()
                .minimumNextBid(auction.getCurrentPrice(), auction.getBidCount());

        proxies.stream()
                .filter(p -> !p.getBidderId().value().equals(auction.getHighestBidderId()))
                .filter(p -> !p.getMaxAmount().isLessThan(minRequired))
                .max(Comparator.comparingLong(p -> p.getMaxAmount().amountInMinorUnits()))
                .ifPresent(proxy -> {
                    String autoKey = "proxy:" + proxy.getBidderId().value()
                            + ":seq:" + (auction.getBidCount() + 1);
                    Bid autoBid = auction.placeBid(proxy.getBidderId(), minRequired, now, autoKey);
                    bidRepository.save(autoBid);
                });
    }
}
