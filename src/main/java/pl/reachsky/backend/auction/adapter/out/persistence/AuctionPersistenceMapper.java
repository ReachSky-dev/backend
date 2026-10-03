package pl.reachsky.backend.auction.adapter.out.persistence;

import org.springframework.stereotype.Component;
import pl.reachsky.backend.auction.domain.AntiSnipingPolicy;
import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.AuctionType;
import pl.reachsky.backend.auction.domain.DutchPricing;
import pl.reachsky.backend.auction.domain.EnglishPricing;
import pl.reachsky.backend.auction.domain.PricingPolicy;
import pl.reachsky.backend.shared.Money;

import java.time.Duration;
import java.util.Currency;

@Component
class AuctionPersistenceMapper {

    AuctionJpaEntity toEntity(Auction a) {
        AuctionJpaEntity e = new AuctionJpaEntity();
        e.id = a.getId().value();
        e.listingId = a.getListingId();
        e.sellerId = a.getSellerId();
        e.type = a.getType();
        e.status = a.getStatus();
        e.startsAt = a.getStartsAt();
        e.endsAt = a.getEndsAt();

        Money reserve = a.getReservePrice();
        e.reservePriceAmount = reserve.amountInMinorUnits();
        e.reservePriceCurrency = reserve.currency().getCurrencyCode();

        PricingPolicy policy = a.getPricingPolicy();
        if (policy instanceof EnglishPricing eng) {
            e.startPriceAmount = eng.startPrice().amountInMinorUnits();
            e.startPriceCurrency = eng.startPrice().currency().getCurrencyCode();
            e.minIncrementAmount = eng.minIncrement().amountInMinorUnits();
            e.minIncrementCurrency = eng.minIncrement().currency().getCurrencyCode();
        } else if (policy instanceof DutchPricing dutch) {
            e.startPriceAmount = dutch.startPrice().amountInMinorUnits();
            e.startPriceCurrency = dutch.startPrice().currency().getCurrencyCode();
            e.decrementAmount = dutch.decrement().amountInMinorUnits();
            e.decrementCurrency = dutch.decrement().currency().getCurrencyCode();
            e.stepSeconds = dutch.step().getSeconds();
            e.floorAmount = dutch.floor().amountInMinorUnits();
            e.floorCurrency = dutch.floor().currency().getCurrencyCode();
        }

        AntiSnipingPolicy asp = a.getAntiSnipingPolicy();
        if (asp != null) {
            e.antiSnipingWindowS = asp.window().getSeconds();
            e.antiSnipingExtS = asp.extension().getSeconds();
            e.antiSnipingMaxExt = asp.maxExtensions();
        }

        e.createdAt = a.getCreatedAt();

        e.currentPriceAmount = a.getCurrentPrice().amountInMinorUnits();
        e.currentPriceCurrency = a.getCurrentPrice().currency().getCurrencyCode();
        e.highestBidderId = a.getHighestBidderId();
        e.bidCount = a.getBidCount();
        e.extensionsUsed = a.getExtensionsUsed();

        return e;
    }

    Auction toDomain(AuctionJpaEntity e) {
        PricingPolicy policy = buildPolicy(e);
        AntiSnipingPolicy asp = buildAntiSniping(e);
        Money currentPrice = new Money(e.currentPriceAmount, Currency.getInstance(e.currentPriceCurrency));
        return Auction.reconstitute(
                new AuctionId(e.id), e.listingId, e.sellerId, e.type, e.status,
                e.startsAt, e.endsAt, policy,
                new Money(e.reservePriceAmount, Currency.getInstance(e.reservePriceCurrency)),
                asp, e.createdAt,
                currentPrice, e.highestBidderId, e.bidCount, e.extensionsUsed);
    }

    private PricingPolicy buildPolicy(AuctionJpaEntity e) {
        Currency currency = Currency.getInstance(e.startPriceCurrency);
        Money startPrice = new Money(e.startPriceAmount, currency);
        if (e.type == AuctionType.ENGLISH) {
            Currency inc = Currency.getInstance(e.minIncrementCurrency);
            return new EnglishPricing(startPrice, new Money(e.minIncrementAmount, inc));
        } else {
            Currency dec = Currency.getInstance(e.decrementCurrency);
            Currency flr = Currency.getInstance(e.floorCurrency);
            return new DutchPricing(
                    startPrice,
                    new Money(e.decrementAmount, dec),
                    Duration.ofSeconds(e.stepSeconds),
                    new Money(e.floorAmount, flr));
        }
    }

    private AntiSnipingPolicy buildAntiSniping(AuctionJpaEntity e) {
        if (e.antiSnipingWindowS == null) return null;
        return new AntiSnipingPolicy(
                Duration.ofSeconds(e.antiSnipingWindowS),
                Duration.ofSeconds(e.antiSnipingExtS),
                e.antiSnipingMaxExt);
    }
}
