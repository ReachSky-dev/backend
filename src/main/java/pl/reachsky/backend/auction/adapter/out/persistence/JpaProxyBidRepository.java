package pl.reachsky.backend.auction.adapter.out.persistence;

import org.springframework.stereotype.Repository;
import pl.reachsky.backend.auction.application.port.out.ProxyBidRepository;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.BidderId;
import pl.reachsky.backend.auction.domain.ProxyBid;

import java.util.List;
import java.util.Optional;

@Repository
class JpaProxyBidRepository implements ProxyBidRepository {

    private final ProxyBidSpringDataRepository springRepo;
    private final BidPersistenceMapper mapper;

    JpaProxyBidRepository(ProxyBidSpringDataRepository springRepo, BidPersistenceMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(ProxyBid proxyBid) {
        springRepo.save(mapper.toEntity(proxyBid));
    }

    @Override
    public Optional<ProxyBid> findByAuctionIdAndBidderId(AuctionId auctionId, BidderId bidderId) {
        return springRepo.findByAuctionIdAndBidderId(auctionId.value(), bidderId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<ProxyBid> findByAuctionId(AuctionId auctionId) {
        return springRepo.findAllByAuctionId(auctionId.value())
                .stream().map(mapper::toDomain).toList();
    }
}
