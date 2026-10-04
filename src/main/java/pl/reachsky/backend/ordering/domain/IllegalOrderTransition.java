package pl.reachsky.backend.ordering.domain;

import pl.reachsky.backend.shared.DomainException;

public class IllegalOrderTransition extends DomainException {

    public IllegalOrderTransition(OrderStatus from, String action) {
        super("Cannot perform '" + action + "' on order in status " + from);
    }
}
