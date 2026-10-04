package pl.reachsky.backend.ordering.adapter.in.rest;

import pl.reachsky.backend.ordering.domain.OrderStatus;

import java.time.Instant;
import java.util.UUID;

record OrderResponse(
        UUID id,
        UUID auctionId,
        UUID buyerId,
        long amountInMinorUnits,
        String currency,
        OrderStatus status,
        Instant paymentDeadline,
        Instant createdAt
) {}
