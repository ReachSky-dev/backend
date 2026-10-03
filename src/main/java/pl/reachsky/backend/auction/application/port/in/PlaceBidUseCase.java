package pl.reachsky.backend.auction.application.port.in;

public interface PlaceBidUseCase {

    BidResult place(PlaceBidCommand cmd);
}
