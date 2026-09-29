package pl.reachsky.backend.identity.adapter.out.persistence;

import org.springframework.stereotype.Component;
import pl.reachsky.backend.identity.domain.UserProfile;
import pl.reachsky.backend.shared.UserId;

@Component
class UserProfilePersistenceMapper {

    UserProfileJpaEntity toEntity(UserProfile p) {
        UserProfileJpaEntity e = new UserProfileJpaEntity();
        e.id = p.getId().value();
        e.subject = p.getSubject();
        e.displayName = p.getDisplayName();
        e.createdAt = p.getCreatedAt();
        return e;
    }

    UserProfile toDomain(UserProfileJpaEntity e) {
        return UserProfile.reconstitute(
                new UserId(e.id),
                e.subject,
                e.displayName,
                e.createdAt);
    }
}
