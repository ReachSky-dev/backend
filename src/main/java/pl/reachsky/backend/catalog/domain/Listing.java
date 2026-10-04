package pl.reachsky.backend.catalog.domain;

import pl.reachsky.backend.shared.Ids;

import java.time.Instant;
import java.util.UUID;

public final class Listing {

    private final ListingId id;
    private final UUID sellerId;
    private final String title;
    private final String description;
    private final ResourceWindow window;
    private final int capacity;
    private ListingStatus status;
    private final Instant createdAt;

    private Listing(ListingId id, UUID sellerId, String title, String description,
                    ResourceWindow window, int capacity, ListingStatus status, Instant createdAt) {
        this.id = id;
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.window = window;
        this.capacity = capacity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Listing create(UUID sellerId, String title, String description,
                                  ResourceWindow window, int capacity) {
        if (sellerId == null) throw new IllegalArgumentException("sellerId must not be null");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
        if (capacity < 1) throw new IllegalArgumentException("capacity must be at least 1");
        return new Listing(
                new ListingId(Ids.next()),
                sellerId,
                title,
                description,
                window,
                capacity,
                ListingStatus.DRAFT,
                Instant.now());
    }

    public static Listing reconstitute(ListingId id, UUID sellerId, String title, String description,
                                        ResourceWindow window, int capacity,
                                        ListingStatus status, Instant createdAt) {
        return new Listing(id, sellerId, title, description, window, capacity, status, createdAt);
    }

    public void publish() {
        this.status = status.publish();
    }

    public void renew() {
        this.status = status.renew();
    }

    public ListingId getId() { return id; }
    public UUID getSellerId() { return sellerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ResourceWindow getWindow() { return window; }
    public int getCapacity() { return capacity; }
    public ListingStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
