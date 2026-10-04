package pl.reachsky.backend.ordering.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.reachsky.backend.ordering.domain.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface OrderSpringDataRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findAllByBuyerId(UUID buyerId);

    boolean existsByAuctionId(UUID auctionId);

    List<OrderJpaEntity> findAllByStatusAndPaymentDeadlineLessThanEqual(OrderStatus status, Instant now);

    List<OrderJpaEntity> findAllByStatus(OrderStatus status);
}
