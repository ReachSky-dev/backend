package pl.reachsky.backend.auction.application.port.out;

import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.AuctionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionRepository {

    void save(Auction auction);

    Optional<Auction> findById(AuctionId id);

    /** Load with PESSIMISTIC_WRITE lock — use inside bid-placement transaction. */
    Optional<Auction> findByIdForUpdate(AuctionId id);

    List<Auction> findByStatus(AuctionStatus status);

    List<Auction> findByListingId(UUID listingId);

    List<Auction> findBySellerId(UUID sellerId);

    List<Auction> findDueToStart(Instant now);

    List<Auction> findDueToEnd(Instant now);
}
