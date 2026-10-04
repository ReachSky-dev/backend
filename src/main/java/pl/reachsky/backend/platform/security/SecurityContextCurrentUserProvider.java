package pl.reachsky.backend.platform.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import pl.reachsky.backend.identity.application.port.in.EnsureUserProfileUseCase;
import pl.reachsky.backend.shared.CurrentUser;
import pl.reachsky.backend.shared.CurrentUserProvider;
import pl.reachsky.backend.shared.UserId;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reads the JWT from SecurityContextHolder, ensures the UserProfile exists,
 * and returns an immutable CurrentUser. Never accessed by domain or application
 * services directly — they depend on the CurrentUserProvider interface only.
 */
@Component
class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    private final EnsureUserProfileUseCase ensureUserProfile;

    SecurityContextCurrentUserProvider(EnsureUserProfileUseCase ensureUserProfile) {
        this.ensureUserProfile = ensureUserProfile;
    }

    @Override
    public CurrentUser get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("Endpoint requires JWT authentication");
        }
        Jwt jwt = jwtAuth.getToken();
        String sub = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        if (username == null || username.isBlank()) username = sub;

        ensureUserProfile.ensure(sub, username);
        return new CurrentUser(new UserId(UUID.fromString(sub)), username, extractRoles(jwt));
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return Set.of();
        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof List<?> list)) return Set.of();
        return list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toUnmodifiableSet());
    }
}
