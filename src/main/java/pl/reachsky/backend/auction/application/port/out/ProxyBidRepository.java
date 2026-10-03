package pl.reachsky.backend.auction.application.port.out;

import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.BidderId;
import pl.reachsky.backend.auction.domain.ProxyBid;

import java.util.List;
import java.util.Optional;

public interface ProxyBidRepository {

    void save(ProxyBid proxyBid);

    Optional<ProxyBid> findByAuctionIdAndBidderId(AuctionId auctionId, BidderId bidderId);

    List<ProxyBid> findByAuctionId(AuctionId auctionId);
}
