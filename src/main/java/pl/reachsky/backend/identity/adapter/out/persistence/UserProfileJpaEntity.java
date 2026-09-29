package pl.reachsky.backend.identity.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
class UserProfileJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    UUID id;

    @Column(nullable = false, length = 255, unique = true)
    String subject;

    @Column(name = "display_name", nullable = false, length = 255)
    String displayName;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    Instant createdAt;

    UserProfileJpaEntity() {}
}
