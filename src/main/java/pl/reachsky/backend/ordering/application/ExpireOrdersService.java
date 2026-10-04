package pl.reachsky.backend.ordering.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.ordering.application.port.out.OrderRepository;
import pl.reachsky.backend.ordering.domain.Order;

import java.time.Instant;
import java.util.List;

/**
 * Finds PENDING_PAYMENT orders past their deadline and marks them EXPIRED (I12).
 */
@Service
public class ExpireOrdersService {

    private final OrderRepository orderRepository;

    ExpireOrdersService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void expireOverdueOrders(Instant now) {
        List<Order> overdue = orderRepository.findOverdue(now);
        for (Order order : overdue) {
            order.expire();
            orderRepository.save(order);
        }
    }
}
