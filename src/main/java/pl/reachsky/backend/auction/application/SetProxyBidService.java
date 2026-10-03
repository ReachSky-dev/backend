package pl.reachsky.backend.auction.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.auction.application.port.in.SetProxyBidCommand;
import pl.reachsky.backend.auction.application.port.in.SetProxyBidUseCase;
import pl.reachsky.backend.auction.application.port.out.AuctionRepository;
import pl.reachsky.backend.auction.application.port.out.ProxyBidRepository;
import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.AuctionStatus;
import pl.reachsky.backend.auction.domain.ProxyBid;
import pl.reachsky.backend.shared.NotFoundException;

import java.time.Clock;
import java.util.Optional;

@Service
class SetProxyBidService implements SetProxyBidUseCase {

    private final AuctionRepository auctionRepository;
    private final ProxyBidRepository proxyBidRepository;
    private final Clock clock;

    SetProxyBidService(AuctionRepository auctionRepository,
                       ProxyBidRepository proxyBidRepository, Clock clock) {
        this.auctionRepository = auctionRepository;
        this.proxyBidRepository = proxyBidRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void set(SetProxyBidCommand cmd) {
        Auction auction = auctionRepository.findById(cmd.auctionId())
                .orElseThrow(() -> new NotFoundException("Auction not found: " + cmd.auctionId().value()));

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new IllegalArgumentException("Proxy bids can only be set on RUNNING auctions");
        }
        if (cmd.bidderId().value().equals(auction.getSellerId())) {
            throw new IllegalArgumentException("Seller cannot set a proxy bid on their own auction");
        }

        // Upsert: replace existing proxy for this bidder
        Optional<ProxyBid> existing = proxyBidRepository
                .findByAuctionIdAndBidderId(cmd.auctionId(), cmd.bidderId());

        ProxyBid proxy = existing
                .map(p -> ProxyBid.reconstitute(p.getId(), p.getAuctionId(), p.getBidderId(),
                        cmd.maxAmount(), p.getCreatedAt()))
                .orElseGet(() -> ProxyBid.create(cmd.auctionId(), cmd.bidderId(),
                        cmd.maxAmount(), clock.instant()));

        proxyBidRepository.save(proxy);
    }
}
