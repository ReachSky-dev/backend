package pl.reachsky.backend.ordering.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.ordering.application.port.out.OrderRepository;
import pl.reachsky.backend.ordering.domain.Order;
import pl.reachsky.backend.platform.outbox.AuctionSoldEvent;
import pl.reachsky.backend.shared.Money;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Currency;

/**
 * Creates an Order when an auction is sold.
 *
 * Idempotent: if an order for this auction already exists (UNIQUE auction_id),
 * the method returns without creating a duplicate.
 *
 * Runs in the same transaction as the OutboxPoller's processEvent, so
 * order creation and outbox marking-as-published are atomic.
 */
@Component
public class CreateOrderOnAuctionSoldListener {

    private static final long PAYMENT_WINDOW_HOURS = 24;

    private final OrderRepository orderRepository;
    private final Clock clock;

    CreateOrderOnAuctionSoldListener(OrderRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    @EventListener
    @Transactional
    public void onAuctionSold(AuctionSoldEvent event) {
        if (orderRepository.existsByAuctionId(event.auctionId())) {
            return; // idempotent
        }
        Order order = Order.create(
                event.auctionId(),
                event.buyerId(),
                new Money(event.amountInMinorUnits(), Currency.getInstance(event.currency())),
                clock.instant().plus(PAYMENT_WINDOW_HOURS, ChronoUnit.HOURS));
        orderRepository.save(order);
    }
}
