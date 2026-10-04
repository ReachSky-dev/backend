package pl.reachsky.backend.auction.application.port.in;

import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.SettlementResult;

public interface SettleAuctionUseCase {

    /** Settles the auction — idempotent. Returns AlreadySettled if already done. */
    SettlementResult settle(AuctionId auctionId);
}
