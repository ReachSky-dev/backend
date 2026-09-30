package pl.reachsky.backend.catalog.adapter.in.rest;

import org.springframework.stereotype.Component;
import pl.reachsky.backend.catalog.application.port.in.CreateListingCommand;
import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ResourceWindow;

import java.util.UUID;

@Component
class ListingWebMapper {

    CreateListingCommand toCommand(CreateListingRequest r, UUID sellerId) {
        return new CreateListingCommand(
                sellerId,
                r.title(),
                r.description(),
                new ResourceWindow(r.windowStart(), r.windowEnd()),
                r.capacity());
    }

    ListingResponse toResponse(Listing l) {
        return new ListingResponse(
                l.getId().value(),
                l.getSellerId(),
                l.getTitle(),
                l.getDescription(),
                l.getWindow().startsAt(),
                l.getWindow().endsAt(),
                l.getCapacity(),
                l.getStatus(),
                l.getCreatedAt());
    }
}
