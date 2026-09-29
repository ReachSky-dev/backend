package pl.reachsky.backend.identity.adapter.out.persistence;

import org.springframework.stereotype.Repository;
import pl.reachsky.backend.identity.application.port.out.UserProfileRepository;
import pl.reachsky.backend.identity.domain.UserProfile;

import java.util.Optional;

@Repository
class JpaUserProfileRepository implements UserProfileRepository {

    private final UserProfileSpringDataRepository springRepo;
    private final UserProfilePersistenceMapper mapper;

    JpaUserProfileRepository(UserProfileSpringDataRepository springRepo,
                              UserProfilePersistenceMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(UserProfile profile) {
        springRepo.save(mapper.toEntity(profile));
    }

    @Override
    public Optional<UserProfile> findBySubject(String subject) {
        return springRepo.findBySubject(subject).map(mapper::toDomain);
    }
}
