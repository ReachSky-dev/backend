package pl.reachsky.backend.auction.application.port.in;

import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.AuctionId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FindAuctionsQuery {

    List<Auction> findRunning();

    Optional<Auction> findById(AuctionId id);

    List<Auction> findByListing(UUID listingId);

    List<Auction> findBySeller(UUID sellerId);

    List<Auction> findEnded();

    List<Auction> findWon(UUID buyerId);
}
