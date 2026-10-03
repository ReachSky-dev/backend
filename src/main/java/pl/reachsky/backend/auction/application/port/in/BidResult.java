package pl.reachsky.backend.auction.application.port.in;

import pl.reachsky.backend.auction.domain.Bid;

public sealed interface BidResult permits BidResult.Accepted, BidResult.Duplicate {

    record Accepted(Bid bid) implements BidResult {}

    /** Same idempotency key already processed — returns the previously created bid. */
    record Duplicate(Bid bid) implements BidResult {}
}
