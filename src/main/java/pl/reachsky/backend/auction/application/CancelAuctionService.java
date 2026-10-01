package pl.reachsky.backend.auction.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.auction.application.port.in.CancelAuctionUseCase;
import pl.reachsky.backend.auction.application.port.out.AuctionRepository;
import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.AuctionId;

import pl.reachsky.backend.shared.NotFoundException;

import java.util.UUID;

@Service
class CancelAuctionService implements CancelAuctionUseCase {

    private final AuctionRepository auctionRepository;

    CancelAuctionService(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }

    @Override
    @Transactional
    public void cancel(AuctionId id, UUID callerId) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Auction not found: " + id.value()));

        if (!auction.getSellerId().equals(callerId)) {
            throw new IllegalArgumentException("Auction does not belong to the caller");
        }

        auction.cancel();
        auctionRepository.save(auction);
    }
}
