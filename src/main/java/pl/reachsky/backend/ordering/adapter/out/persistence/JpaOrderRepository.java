package pl.reachsky.backend.ordering.adapter.out.persistence;

import org.springframework.stereotype.Repository;
import pl.reachsky.backend.ordering.application.port.out.OrderRepository;
import pl.reachsky.backend.ordering.domain.Order;
import pl.reachsky.backend.ordering.domain.OrderId;
import pl.reachsky.backend.ordering.domain.OrderStatus;
import pl.reachsky.backend.shared.Money;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaOrderRepository implements OrderRepository {

    private final OrderSpringDataRepository springRepo;

    JpaOrderRepository(OrderSpringDataRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public void save(Order order) {
        springRepo.save(toEntity(order));
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return springRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Order> findByBuyerId(UUID buyerId) {
        return springRepo.findAllByBuyerId(buyerId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByAuctionId(UUID auctionId) {
        return springRepo.existsByAuctionId(auctionId);
    }

    @Override
    public List<Order> findOverdue(Instant now) {
        return springRepo.findAllByStatusAndPaymentDeadlineLessThanEqual(OrderStatus.PENDING_PAYMENT, now)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return springRepo.findAllByStatus(status).stream().map(this::toDomain).toList();
    }

    private OrderJpaEntity toEntity(Order o) {
        OrderJpaEntity e = new OrderJpaEntity();
        e.id = o.getId().value();
        e.auctionId = o.getAuctionId();
        e.buyerId = o.getBuyerId();
        e.amount = o.getAmount().amountInMinorUnits();
        e.currency = o.getAmount().currency().getCurrencyCode();
        e.status = o.getStatus();
        e.paymentDeadline = o.getPaymentDeadline();
        e.createdAt = o.getCreatedAt();
        return e;
    }

    private Order toDomain(OrderJpaEntity e) {
        return Order.reconstitute(
                new OrderId(e.id), e.auctionId, e.buyerId,
                new Money(e.amount, Currency.getInstance(e.currency)),
                e.status, e.paymentDeadline, e.createdAt);
    }
}
