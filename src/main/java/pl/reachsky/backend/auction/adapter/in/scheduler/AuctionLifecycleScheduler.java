package pl.reachsky.backend.auction.adapter.in.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.reachsky.backend.auction.application.AuctionLifecycleService;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "reachsky.scheduler.enabled", havingValue = "true", matchIfMissing = true)
class AuctionLifecycleScheduler {

    private final AuctionLifecycleService lifecycleService;

    AuctionLifecycleScheduler(AuctionLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @Scheduled(fixedDelay = 10_000)
    void tick() {
        lifecycleService.startDueAuctions(Instant.now());
    }
}
