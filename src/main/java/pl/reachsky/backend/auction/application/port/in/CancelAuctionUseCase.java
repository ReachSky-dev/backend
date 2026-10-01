package pl.reachsky.backend.auction.application.port.in;

import pl.reachsky.backend.auction.domain.AuctionId;

public interface CancelAuctionUseCase {

    void cancel(AuctionId id, java.util.UUID callerId);
}
