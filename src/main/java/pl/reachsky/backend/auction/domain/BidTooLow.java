package pl.reachsky.backend.auction.domain;

import pl.reachsky.backend.shared.DomainException;
import pl.reachsky.backend.shared.Money;

public class BidTooLow extends DomainException {

    public BidTooLow(Money attempted, Money required) {
        super("Bid " + attempted.amountInMinorUnits() + " is below minimum required " + required.amountInMinorUnits()
                + " " + required.currency().getCurrencyCode());
    }
}
