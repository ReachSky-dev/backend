package pl.reachsky.backend.ordering.adapter.in.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.reachsky.backend.ordering.application.ExpireOrdersService;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "reachsky.scheduler.enabled", havingValue = "true", matchIfMissing = true)
class ExpireOrdersScheduler {

    private final ExpireOrdersService expireOrdersService;

    ExpireOrdersScheduler(ExpireOrdersService expireOrdersService) {
        this.expireOrdersService = expireOrdersService;
    }

    @Scheduled(fixedDelay = 60_000)
    void tick() {
        expireOrdersService.expireOverdueOrders(Instant.now());
    }
}
