package pl.reachsky.backend.catalog.adapter.in.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.reachsky.backend.catalog.application.ExpireListingsService;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "reachsky.scheduler.enabled", havingValue = "true", matchIfMissing = true)
class ListingExpiryScheduler implements ApplicationListener<ApplicationReadyEvent> {

    private final ExpireListingsService service;

    ListingExpiryScheduler(ExpireListingsService service) {
        this.service = service;
    }

    @Scheduled(fixedDelay = 60_000)
    void tick() {
        service.expireDueListings(Instant.now());
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        service.expireDueListings(Instant.now());
    }
}
