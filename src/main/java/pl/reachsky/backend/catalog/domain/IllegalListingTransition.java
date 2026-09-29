package pl.reachsky.backend.catalog.domain;

import pl.reachsky.backend.shared.DomainException;

public class IllegalListingTransition extends DomainException {

    public IllegalListingTransition(ListingStatus from, String operation) {
        super("Cannot perform '" + operation + "' on listing in status " + from);
    }
}
