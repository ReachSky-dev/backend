package pl.reachsky.backend.auction.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.auction.application.port.in.SettleAuctionUseCase;
import pl.reachsky.backend.auction.application.port.out.AuctionRepository;
import pl.reachsky.backend.auction.domain.Auction;
import pl.reachsky.backend.auction.domain.AuctionId;
import pl.reachsky.backend.auction.domain.SettlementResult;
import pl.reachsky.backend.platform.outbox.OutboxEvent;
import pl.reachsky.backend.platform.outbox.OutboxRepository;
import pl.reachsky.backend.shared.NotFoundException;

import java.time.Clock;
import java.util.UUID;

/**
 * Settles an auction atomically: transitions status to SOLD or RESERVE_NOT_MET,
 * saves the auction, and writes an outbox event — all in a single transaction.
 *
 * Idempotent: if the auction is already in a terminal state, returns AlreadySettled
 * without writing another outbox event.
 */
@Service
public class SettleAuctionService implements SettleAuctionUseCase {

    private final AuctionRepository auctionRepository;
    private final OutboxRepository outboxRepository;
    private final Clock clock;

    SettleAuctionService(AuctionRepository auctionRepository,
                         OutboxRepository outboxRepository,
                         Clock clock) {
        this.auctionRepository = auctionRepository;
        this.outboxRepository = outboxRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SettlementResult settle(AuctionId auctionId) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new NotFoundException("Auction not found: " + auctionId.value()));

        SettlementResult result = auction.settle();

        if (result instanceof SettlementResult.AlreadySettled) {
            return result;
        }

        auctionRepository.save(auction);

        OutboxEvent event = buildOutboxEvent(auctionId, auction.getListingId(), result);
        outboxRepository.save(event);

        return result;
    }

    private OutboxEvent buildOutboxEvent(AuctionId auctionId, UUID listingId, SettlementResult result) {
        String payload = switch (result) {
            case SettlementResult.Sold sold -> String.format(
                    "{\"auctionId\":\"%s\",\"listingId\":\"%s\",\"winnerId\":\"%s\",\"amountInMinorUnits\":%d,\"currency\":\"%s\"}",
                    auctionId.value(), listingId, sold.winnerId(),
                    sold.amount().amountInMinorUnits(), sold.amount().currency().getCurrencyCode());
            case SettlementResult.ReserveNotMet rnm -> String.format(
                    "{\"auctionId\":\"%s\",\"listingId\":\"%s\"}", auctionId.value(), listingId);
            case SettlementResult.AlreadySettled a -> throw new IllegalStateException("unreachable");
        };

        String eventType = switch (result) {
            case SettlementResult.Sold s -> "AUCTION_SOLD";
            case SettlementResult.ReserveNotMet r -> "AUCTION_RESERVE_NOT_MET";
            case SettlementResult.AlreadySettled a -> throw new IllegalStateException("unreachable");
        };

        return OutboxEvent.create("Auction", auctionId.value().toString(), eventType,
                payload, clock.instant());
    }
}
