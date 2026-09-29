package pl.reachsky.backend.identity.application.port.in;

import pl.reachsky.backend.identity.domain.UserProfile;

public interface EnsureUserProfileUseCase {

    /**
     * Returns the existing UserProfile for {@code subject}, or creates one.
     * Idempotent: multiple calls with the same {@code subject} always return
     * the same profile.
     */
    UserProfile ensure(String subject, String displayName);
}
