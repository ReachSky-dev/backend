package pl.reachsky.backend.auction.application.port.in;

import pl.reachsky.backend.auction.domain.AuctionId;

public interface CreateAuctionUseCase {

    AuctionId create(CreateAuctionCommand command);
}
