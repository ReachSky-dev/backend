package pl.reachsky.backend.auction.adapter.in.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.reachsky.backend.auction.application.AuctionLifecycleService;

import java.time.Instant;

/**
 * Periodically settles auctions whose end time has passed.
 * Also runs reconciliation on startup (I11) to catch any auctions
 * that ended while the application was down.
 */
@Component
@ConditionalOnProperty(name = "reachsky.scheduler.enabled", havingValue = "true", matchIfMissing = true)
class AuctionSettlementScheduler implements ApplicationListener<ApplicationReadyEvent> {

    private final AuctionLifecycleService lifecycleService;

    AuctionSettlementScheduler(AuctionLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @Scheduled(fixedDelay = 5_000)
    void tick() {
        lifecycleService.settleDueAuctions(Instant.now());
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        lifecycleService.settleDueAuctions(Instant.now());
        lifecycleService.reconcileListingStatuses();
    }
}
