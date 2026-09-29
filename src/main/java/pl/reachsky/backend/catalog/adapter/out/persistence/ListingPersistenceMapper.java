package pl.reachsky.backend.catalog.adapter.out.persistence;

import org.springframework.stereotype.Component;
import pl.reachsky.backend.catalog.domain.Listing;
import pl.reachsky.backend.catalog.domain.ListingId;
import pl.reachsky.backend.catalog.domain.ResourceWindow;

@Component
class ListingPersistenceMapper {

    ListingJpaEntity toEntity(Listing l) {
        ListingJpaEntity e = new ListingJpaEntity();
        e.id = l.getId().value();
        e.sellerId = l.getSellerId();
        e.title = l.getTitle();
        e.description = l.getDescription();
        e.windowStartsAt = l.getWindow().startsAt();
        e.windowEndsAt = l.getWindow().endsAt();
        e.capacity = l.getCapacity();
        e.status = l.getStatus();
        e.createdAt = l.getCreatedAt();
        return e;
    }

    Listing toDomain(ListingJpaEntity e) {
        return Listing.reconstitute(
                new ListingId(e.id),
                e.sellerId,
                e.title,
                e.description,
                new ResourceWindow(e.windowStartsAt, e.windowEndsAt),
                e.capacity,
                e.status,
                e.createdAt);
    }
}
