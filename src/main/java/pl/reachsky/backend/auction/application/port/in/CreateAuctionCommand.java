package pl.reachsky.backend.auction.application.port.in;

import pl.reachsky.backend.auction.domain.AntiSnipingPolicy;
import pl.reachsky.backend.auction.domain.AuctionType;
import pl.reachsky.backend.auction.domain.PricingPolicy;
import pl.reachsky.backend.shared.Money;

import java.time.Instant;
import java.util.UUID;

public record CreateAuctionCommand(
        UUID sellerId,
        UUID listingId,
        AuctionType type,
        Instant startsAt,
        Instant endsAt,
        PricingPolicy pricingPolicy,
        Money reservePrice,
        AntiSnipingPolicy antiSnipingPolicy
) {}
