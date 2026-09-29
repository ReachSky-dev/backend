package pl.reachsky.backend.identity.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.reachsky.backend.identity.application.port.in.EnsureUserProfileUseCase;
import pl.reachsky.backend.identity.application.port.out.UserProfileRepository;
import pl.reachsky.backend.identity.domain.UserProfile;

@Service
class EnsureUserProfileService implements EnsureUserProfileUseCase {

    private final UserProfileRepository repository;

    EnsureUserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public UserProfile ensure(String subject, String displayName) {
        return repository.findBySubject(subject)
                .orElseGet(() -> {
                    UserProfile profile = UserProfile.create(subject, displayName);
                    repository.save(profile);
                    return profile;
                });
    }
}
