package pl.reachsky.backend.auction.application.port.in;

import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.BidderId;
import pl.reachsky.backend.shared.Money;

public record SetProxyBidCommand(
        AuctionId auctionId,
        BidderId bidderId,
        Money maxAmount
) {}
