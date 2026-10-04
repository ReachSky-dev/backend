package pl.reachsky.backend.ordering.application.port.out;

import pl.reachsky.backend.ordering.domain.Order;
import pl.reachsky.backend.ordering.domain.OrderId;
import pl.reachsky.backend.ordering.domain.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(OrderId id);

    List<Order> findByBuyerId(UUID buyerId);

    boolean existsByAuctionId(UUID auctionId);

    /** PENDING_PAYMENT orders whose paymentDeadline <= now. */
    List<Order> findOverdue(Instant now);

    List<Order> findByStatus(OrderStatus status);
}
