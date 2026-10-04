package pl.reachsky.backend.ordering.application.port.in;

import pl.reachsky.backend.ordering.domain.OrderId;

public interface PayOrderUseCase {

    void pay(OrderId orderId);
}
