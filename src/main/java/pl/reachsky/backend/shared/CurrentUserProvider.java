package pl.reachsky.backend.shared;

/**
 * Port: returns the currently authenticated user.
 * Interface lives in shared/ so any application service can depend on it
 * without importing from platform (which would break the dependency direction).
 * Implementation lives in platform/security/ and reads from SecurityContextHolder.
 */
public interface CurrentUserProvider {

    CurrentUser get();
}
