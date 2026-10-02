package pl.reachsky.backend.shared;

/**
 * Thrown when a JWT access token does not carry a {@code sub} claim.
 * Keycloak 26 requires the {@code basic} scope in defaultClientScopes — without it
 * the access token omits {@code sub} and no user profile can be established.
 * Maps to HTTP 401 in GlobalExceptionHandler.
 */
public class MissingSubjectException extends DomainException {

    public MissingSubjectException() {
        super("JWT access token is missing the 'sub' claim — ensure the 'basic' scope is included in defaultClientScopes");
    }
}
