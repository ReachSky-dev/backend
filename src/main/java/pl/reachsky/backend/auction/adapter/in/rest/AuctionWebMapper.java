package pl.reachsky.backend.auction.adapter.in.rest;

import org.springframework.stereotype.Component;
import pl.reachsky.backend.auction.application.port.in.CreateAuctionCommand;
import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.AuctionType;
import pl.reachsky.backend.auction.domain.DutchPricing;
import pl.reachsky.backend.auction.domain.EnglishPricing;
import pl.reachsky.backend.auction.domain.PricingPolicy;
import pl.reachsky.backend.shared.Money;

import java.time.Duration;
import java.time.Instant;
import java.util.Currency;

@Component
class AuctionWebMapper {

    CreateAuctionCommand toCommand(CreateAuctionRequest req, java.util.UUID sellerId) {
        PricingPolicy policy = buildPricingPolicy(req);
        return new CreateAuctionCommand(
                sellerId,
                req.listingId(),
                req.type(),
                req.startsAt(),
                req.endsAt(),
                policy,
                Money.of(req.reservePriceAmount(), req.currency()),
                null);
    }

    AuctionResponse toResponse(Auction auction) {
        Instant now = Instant.now();
        Money currentPrice = auction.currentPriceAt(now);

        Long decrementAmount = null;
        String decrementCurrency = null;
        Long stepSeconds = null;

        if (auction.getPricingPolicy() instanceof DutchPricing dutch) {
            decrementAmount = dutch.decrement().amountInMinorUnits();
            decrementCurrency = dutch.decrement().currency().getCurrencyCode();
            stepSeconds = dutch.step().getSeconds();
        }

        return new AuctionResponse(
                auction.getId().value(),
                auction.getListingId(),
                auction.getSellerId(),
                auction.getType(),
                auction.getStatus(),
                auction.getStartsAt(),
                auction.getEndsAt(),
                currentPrice.amountInMinorUnits(),
                currentPrice.currency().getCurrencyCode(),
                decrementAmount,
                decrementCurrency,
                stepSeconds,
                auction.getCreatedAt());
    }

    private PricingPolicy buildPricingPolicy(CreateAuctionRequest req) {
        Currency currency = Currency.getInstance(req.currency());
        Money startPrice = new Money(req.startPriceAmount(), currency);

        if (req.type() == AuctionType.ENGLISH) {
            if (req.minIncrementAmount() == null) {
                throw new IllegalArgumentException("minIncrementAmount is required for ENGLISH auctions");
            }
            return new EnglishPricing(startPrice, new Money(req.minIncrementAmount(), currency));
        } else {
            if (req.decrementAmount() == null || req.stepSeconds() == null || req.floorAmount() == null) {
                throw new IllegalArgumentException(
                        "decrementAmount, stepSeconds and floorAmount are required for DUTCH auctions");
            }
            return new DutchPricing(
                    startPrice,
                    new Money(req.decrementAmount(), currency),
                    Duration.ofSeconds(req.stepSeconds()),
                    new Money(req.floorAmount(), currency));
        }
    }
}
