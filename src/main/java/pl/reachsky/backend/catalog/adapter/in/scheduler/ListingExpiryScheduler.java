package pl.reachsky.backend.catalog.adapter.in.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.reachsky.backend.catalog.application.CloseExpiredListingsService;

@Component
@ConditionalOnProperty(name = "reachsky.scheduler.enabled", havingValue = "true", matchIfMissing = true)
class ListingExpiryScheduler {

    private final CloseExpiredListingsService service;

    ListingExpiryScheduler(CloseExpiredListingsService service) {
        this.service = service;
    }

    @Scheduled(fixedDelay = 60_000)
    void tick() {
        service.closeExpired();
    }
}
