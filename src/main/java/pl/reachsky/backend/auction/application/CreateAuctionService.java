package pl.reachsky.backend.auction.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.auction.application.port.in.CreateAuctionCommand;
import pl.reachsky.backend.auction.application.port.in.CreateAuctionUseCase;
import pl.reachsky.backend.auction.application.port.out.AuctionRepository;
import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.catalog.application.port.in.FindListingsQuery;
import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingId;
import pl.reachsky.backend.catalog.domain.ListingStatus;

import java.time.Instant;

@Service
class CreateAuctionService implements CreateAuctionUseCase {

    private final AuctionRepository auctionRepository;
    private final FindListingsQuery findListingsQuery;

    CreateAuctionService(AuctionRepository auctionRepository, FindListingsQuery findListingsQuery) {
        this.auctionRepository = auctionRepository;
        this.findListingsQuery = findListingsQuery;
    }

    @Override
    @Transactional
    public AuctionId create(CreateAuctionCommand cmd) {
        Listing listing = findListingsQuery.findById(new ListingId(cmd.listingId()))
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + cmd.listingId()));

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new IllegalArgumentException("Listing must be ACTIVE to create an auction");
        }
        if (!listing.getSellerId().equals(cmd.sellerId())) {
            throw new IllegalArgumentException("Listing does not belong to the caller");
        }

        Instant now = Instant.now();
        if (!cmd.startsAt().isAfter(now)) {
            throw new IllegalArgumentException("startsAt must be in the future");
        }
        if (!cmd.endsAt().isAfter(cmd.startsAt())) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }

        Auction auction = Auction.create(
                cmd.listingId(), cmd.sellerId(), cmd.type(),
                cmd.startsAt(), cmd.endsAt(),
                cmd.pricingPolicy(), cmd.reservePrice(),
                cmd.antiSnipingPolicy());
        auction.schedule();
        auctionRepository.save(auction);
        return auction.getId();
    }
}
