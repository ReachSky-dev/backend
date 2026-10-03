package pl.reachsky.backend.auction.adapter.out.persistence;

import org.springframework.stereotype.Repository;
import pl.reachsky.backend.auction.application.port.out.BidRepository;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.Bid;

import java.util.List;
import java.util.Optional;

@Repository
class JpaBidRepository implements BidRepository {

    private final BidSpringDataRepository springRepo;
    private final BidPersistenceMapper mapper;

    JpaBidRepository(BidSpringDataRepository springRepo, BidPersistenceMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(Bid bid) {
        springRepo.save(mapper.toEntity(bid));
    }

    @Override
    public Optional<Bid> findByIdempotencyKey(String idempotencyKey) {
        return springRepo.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public List<Bid> findByAuctionIdOrderBySequenceAsc(AuctionId auctionId) {
        return springRepo.findAllByAuctionIdOrderBySequenceAsc(auctionId.value())
                .stream().map(mapper::toDomain).toList();
    }
}
