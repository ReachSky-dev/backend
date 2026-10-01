package pl.reachsky.backend.auction.adapter.out.persistence;

import org.springframework.stereotype.Repository;
import pl.reachsky.backend.auction.application.port.out.AuctionRepository;
import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.AuctionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaAuctionRepository implements AuctionRepository {

    private final AuctionSpringDataRepository springRepo;
    private final AuctionPersistenceMapper mapper;

    JpaAuctionRepository(AuctionSpringDataRepository springRepo, AuctionPersistenceMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(Auction auction) {
        springRepo.save(mapper.toEntity(auction));
    }

    @Override
    public Optional<Auction> findById(AuctionId id) {
        return springRepo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Auction> findByStatus(AuctionStatus status) {
        return springRepo.findAllByStatus(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Auction> findByListingId(UUID listingId) {
        return springRepo.findAllByListingId(listingId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Auction> findDueToStart(Instant now) {
        return springRepo.findAllByStatusAndStartsAtLessThanEqual(AuctionStatus.SCHEDULED, now)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Auction> findDueToEnd(Instant now) {
        return springRepo.findAllByStatusAndEndsAtLessThanEqual(AuctionStatus.RUNNING, now)
                .stream().map(mapper::toDomain).toList();
    }
}
