package pl.reachsky.backend.auction.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.auction.application.port.in.FindBidsQuery;
import pl.reachsky.backend.auction.application.port.out.BidRepository;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.Bid;

import java.util.List;

@Service
class FindBidsService implements FindBidsQuery {

    private final BidRepository bidRepository;

    FindBidsService(BidRepository bidRepository) {
        this.bidRepository = bidRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bid> findByAuction(AuctionId auctionId) {
        return bidRepository.findByAuctionIdOrderBySequenceAsc(auctionId);
    }
}
