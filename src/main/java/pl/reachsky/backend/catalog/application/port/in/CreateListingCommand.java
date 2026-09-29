package pl.reachsky.backend.catalog.application.port.in;

import pl.reachsky.backend.catalog.domain.ResourceWindow;

import java.util.UUID;

public record CreateListingCommand(
        UUID sellerId,
        String title,
        String description,
        ResourceWindow window,
        int capacity
) {}
