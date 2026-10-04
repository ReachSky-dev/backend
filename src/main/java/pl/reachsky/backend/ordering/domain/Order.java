package pl.reachsky.backend.ordering.domain;

import pl.reachsky.backend.shared.Ids;
import pl.reachsky.backend.shared.Money;

import java.time.Instant;
import java.util.UUID;

public final class Order {

    private final OrderId id;
    private final UUID auctionId;
    private final UUID buyerId;
    private final Money amount;
    private OrderStatus status;
    private final Instant paymentDeadline;
    private final Instant createdAt;

    private Order(OrderId id, UUID auctionId, UUID buyerId, Money amount,
                  OrderStatus status, Instant paymentDeadline, Instant createdAt) {
        this.id = id;
        this.auctionId = auctionId;
        this.buyerId = buyerId;
        this.amount = amount;
        this.status = status;
        this.paymentDeadline = paymentDeadline;
        this.createdAt = createdAt;
    }

    public static Order create(UUID auctionId, UUID buyerId, Money amount, Instant paymentDeadline) {
        return new Order(new OrderId(Ids.next()), auctionId, buyerId, amount,
                OrderStatus.PENDING_PAYMENT, paymentDeadline, Instant.now());
    }

    public static Order reconstitute(OrderId id, UUID auctionId, UUID buyerId, Money amount,
                                     OrderStatus status, Instant paymentDeadline, Instant createdAt) {
        return new Order(id, auctionId, buyerId, amount, status, paymentDeadline, createdAt);
    }

    /** Marks the order as paid. Throws if not in PENDING_PAYMENT or deadline has passed. */
    public void pay(Instant now) {
        if (status != OrderStatus.PENDING_PAYMENT) throw new IllegalOrderTransition(status, "pay");
        if (now.isAfter(paymentDeadline)) throw new OrderPaymentExpired(id);
        status = OrderStatus.PAID;
    }

    /** Expires the order. No-op if already in a terminal state. */
    public void expire() {
        if (status != OrderStatus.PENDING_PAYMENT) return;
        status = OrderStatus.EXPIRED;
    }

    public OrderId getId() { return id; }
    public UUID getAuctionId() { return auctionId; }
    public UUID getBuyerId() { return buyerId; }
    public Money getAmount() { return amount; }
    public OrderStatus getStatus() { return status; }
    public Instant getPaymentDeadline() { return paymentDeadline; }
    public Instant getCreatedAt() { return createdAt; }
}
