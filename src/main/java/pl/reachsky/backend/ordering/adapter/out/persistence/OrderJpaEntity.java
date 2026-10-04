package pl.reachsky.backend.ordering.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pl.reachsky.backend.ordering.domain.OrderStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
class OrderJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    UUID id;

    @Column(name = "auction_id", nullable = false, columnDefinition = "uuid")
    UUID auctionId;

    @Column(name = "buyer_id", nullable = false, columnDefinition = "uuid")
    UUID buyerId;

    @Column(nullable = false)
    long amount;

    @Column(nullable = false, length = 3)
    String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    OrderStatus status;

    @Column(name = "payment_deadline", nullable = false, columnDefinition = "timestamptz")
    Instant paymentDeadline;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    OrderJpaEntity() {}
}
