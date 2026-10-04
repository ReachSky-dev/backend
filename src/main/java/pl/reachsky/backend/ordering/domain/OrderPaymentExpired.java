package pl.reachsky.backend.ordering.domain;

import pl.reachsky.backend.shared.DomainException;

public class OrderPaymentExpired extends DomainException {

    public OrderPaymentExpired(OrderId id) {
        super("Order " + id.value() + " payment deadline has passed");
    }
}
