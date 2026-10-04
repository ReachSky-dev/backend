package pl.reachsky.backend.ordering.application;

import org.springframework.stereotype.Service;
import pl.reachsky.backend.ordering.application.port.in.FindOrdersQuery;
import pl.reachsky.backend.ordering.application.port.out.OrderRepository;
import pl.reachsky.backend.ordering.domain.Order;
import pl.reachsky.backend.ordering.domain.OrderId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class FindOrdersService implements FindOrdersQuery {

    private final OrderRepository orderRepository;

    FindOrdersService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return orderRepository.findById(id);
    }

    @Override
    public List<Order> findByBuyer(UUID buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }
}
