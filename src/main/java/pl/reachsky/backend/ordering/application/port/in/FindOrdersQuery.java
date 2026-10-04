package pl.reachsky.backend.ordering.application.port.in;

import pl.reachsky.backend.ordering.domain.Order;
import pl.reachsky.backend.ordering.domain.OrderId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FindOrdersQuery {

    Optional<Order> findById(OrderId id);

    List<Order> findByBuyer(UUID buyerId);
}
