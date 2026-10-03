package pl.reachsky.backend.auction.application.port.out;

import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.Bid;

import java.util.List;
import java.util.Optional;

public interface BidRepository {

    void save(Bid bid);

    Optional<Bid> findByIdempotencyKey(String idempotencyKey);

    List<Bid> findByAuctionIdOrderBySequenceAsc(AuctionId auctionId);
}
