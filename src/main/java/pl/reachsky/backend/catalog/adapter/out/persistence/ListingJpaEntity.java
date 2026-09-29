package pl.reachsky.backend.catalog.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pl.reachsky.backend.catalog.domain.ListingStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listings")
class ListingJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    UUID id;

    @Column(name = "seller_id", nullable = false, columnDefinition = "uuid")
    UUID sellerId;

    @Column(nullable = false, length = 255)
    String title;

    @Column(columnDefinition = "text")
    String description;

    @Column(name = "window_starts_at", nullable = false, columnDefinition = "timestamptz")
    Instant windowStartsAt;

    @Column(name = "window_ends_at", nullable = false, columnDefinition = "timestamptz")
    Instant windowEndsAt;

    @Column(nullable = false)
    int capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ListingStatus status;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    ListingJpaEntity() {}
}
