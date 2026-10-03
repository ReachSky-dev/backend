package pl.reachsky.backend.auction.application.port.in;

import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.Bid;

import java.util.List;

public interface FindBidsQuery {

    List<Bid> findByAuction(AuctionId auctionId);
}
