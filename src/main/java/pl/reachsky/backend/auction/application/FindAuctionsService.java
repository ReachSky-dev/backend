package pl.reachsky.backend.auction.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.auction.application.port.in.FindAuctionsQuery;
import pl.reachsky.backend.auction.application.port.out.AuctionRepository;
import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.AuctionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class FindAuctionsService implements FindAuctionsQuery {

    private final AuctionRepository auctionRepository;

    FindAuctionsService(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Auction> findRunning() {
        return auctionRepository.findByStatus(AuctionStatus.RUNNING);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Auction> findById(AuctionId id) {
        return auctionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Auction> findByListing(UUID listingId) {
        return auctionRepository.findByListingId(listingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Auction> findBySeller(UUID sellerId) {
        return auctionRepository.findBySellerId(sellerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Auction> findEnded() {
        return auctionRepository.findEnded();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Auction> findWon(UUID buyerId) {
        return auctionRepository.findByWinnerId(buyerId);
    }
}
