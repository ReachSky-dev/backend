package pl.reachsky.backend.shared;

public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    /** SCREAMING_SNAKE_CASE error code derived from the concrete class name. */
    public String getCode() {
        return getClass().getSimpleName()
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase();
    }
}
