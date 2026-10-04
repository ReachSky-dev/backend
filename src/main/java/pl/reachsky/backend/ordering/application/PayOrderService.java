package pl.reachsky.backend.ordering.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.ordering.application.port.in.PayOrderUseCase;
import pl.reachsky.backend.ordering.application.port.out.OrderRepository;
import pl.reachsky.backend.ordering.domain.Order;
import pl.reachsky.backend.ordering.domain.OrderId;
import pl.reachsky.backend.shared.NotFoundException;

import java.time.Clock;

@Service
class PayOrderService implements PayOrderUseCase {

    private final OrderRepository orderRepository;
    private final Clock clock;

    PayOrderService(OrderRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void pay(OrderId orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId.value()));
        order.pay(clock.instant());
        orderRepository.save(order);
    }
}
