package pl.reachsky.backend.ordering.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.reachsky.backend.AbstractIntegrationTest;
import pl.reachsky.backend.ordering.application.port.out.OrderRepository;
import pl.reachsky.backend.ordering.domain.Order;
import pl.reachsky.backend.ordering.domain.OrderId;
import pl.reachsky.backend.ordering.domain.OrderStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies invariant I12: PENDING_PAYMENT orders past their deadline are expired by
 * the scheduler/service, releasing the buyer from obligation.
 */
@SpringBootTest
class OrderExpiryTest extends AbstractIntegrationTest {

    @Autowired
    ExpireOrdersService expireOrdersService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE orders");
    }

    @Test
    void overdueOrder_isExpired() {
        Instant overdueDeadline = Instant.now().minus(2, ChronoUnit.HOURS);
        Order order = insertOrder(UUID.randomUUID(), overdueDeadline);

        expireOrdersService.expireOverdueOrders(Instant.now());

        Order updated = orderRepository.findById(order.getId()).orElseThrow();

        System.out.println("=== OrderExpiryTest (I12) ===");
        System.out.println("Status before: PENDING_PAYMENT");
        System.out.println("Status after:  " + updated.getStatus());
        System.out.println("============================");

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.EXPIRED);
    }

    @Test
    void nonOverdueOrder_isNotExpired() {
        Instant futureDeadline = Instant.now().plus(24, ChronoUnit.HOURS);
        Order order = insertOrder(UUID.randomUUID(), futureDeadline);

        expireOrdersService.expireOverdueOrders(Instant.now());

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void expiry_isIdempotent() {
        Instant overdueDeadline = Instant.now().minus(1, ChronoUnit.HOURS);
        Order order = insertOrder(UUID.randomUUID(), overdueDeadline);

        expireOrdersService.expireOverdueOrders(Instant.now());
        expireOrdersService.expireOverdueOrders(Instant.now());
        expireOrdersService.expireOverdueOrders(Instant.now());

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.EXPIRED);
    }

    // -------------------------------------------------------------------------

    private Order insertOrder(UUID auctionId, Instant paymentDeadline) {
        UUID id = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO orders (id, auction_id, buyer_id, amount, currency, status, payment_deadline, created_at)
                VALUES (?::uuid, ?::uuid, ?::uuid, 10000, 'PLN', 'PENDING_PAYMENT', ?, now())
                """, id, auctionId, buyerId,
                java.sql.Timestamp.from(paymentDeadline));
        return orderRepository.findById(new OrderId(id)).orElseThrow();
    }
}
