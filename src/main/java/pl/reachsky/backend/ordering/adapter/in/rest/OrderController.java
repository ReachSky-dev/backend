package pl.reachsky.backend.ordering.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.reachsky.backend.ordering.application.port.in.FindOrdersQuery;
import pl.reachsky.backend.ordering.application.port.in.PayOrderUseCase;
import pl.reachsky.backend.ordering.domain.Order;
import pl.reachsky.backend.ordering.domain.OrderId;
import pl.reachsky.backend.shared.CurrentUserProvider;
import pl.reachsky.backend.shared.NotFoundException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final FindOrdersQuery findOrders;
    private final PayOrderUseCase payOrder;
    private final CurrentUserProvider currentUserProvider;

    OrderController(FindOrdersQuery findOrders, PayOrderUseCase payOrder,
                    CurrentUserProvider currentUserProvider) {
        this.findOrders = findOrders;
        this.payOrder = payOrder;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "List orders for the current buyer")
    List<OrderResponse> myOrders() {
        UUID buyerId = currentUserProvider.get().id().value();
        return findOrders.findByBuyer(buyerId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    @ApiResponse(responseCode = "404", description = "Order not found")
    OrderResponse getById(@PathVariable UUID id) {
        return findOrders.findById(new OrderId(id))
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Pay for an order")
    @ApiResponse(responseCode = "409", description = "Order not in PENDING_PAYMENT state or deadline passed")
    @ApiResponse(responseCode = "404", description = "Order not found")
    OrderResponse pay(@PathVariable UUID id) {
        payOrder.pay(new OrderId(id));
        return findOrders.findById(new OrderId(id))
                .map(this::toResponse)
                .orElseThrow();
    }

    private OrderResponse toResponse(Order o) {
        return new OrderResponse(
                o.getId().value(),
                o.getAuctionId(),
                o.getBuyerId(),
                o.getAmount().amountInMinorUnits(),
                o.getAmount().currency().getCurrencyCode(),
                o.getStatus(),
                o.getPaymentDeadline(),
                o.getCreatedAt());
    }
}
