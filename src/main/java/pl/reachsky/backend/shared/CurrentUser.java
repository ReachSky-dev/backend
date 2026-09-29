package pl.reachsky.backend.shared;

import java.util.Set;

/**
 * Immutable view of the authenticated principal — derived from JWT claims and
 * the persisted UserProfile. Lives in shared/ because multiple modules
 * (catalog, identity, …) need it without coupling to each other.
 */
public record CurrentUser(UserId id, String username, Set<String> roles) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
