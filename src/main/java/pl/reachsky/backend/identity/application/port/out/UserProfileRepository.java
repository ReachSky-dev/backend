package pl.reachsky.backend.identity.application.port.out;

import pl.reachsky.backend.identity.domain.UserProfile;

import java.util.Optional;

public interface UserProfileRepository {

    void save(UserProfile profile);

    Optional<UserProfile> findBySubject(String subject);
}
