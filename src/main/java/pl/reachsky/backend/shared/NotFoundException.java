package pl.reachsky.backend.shared;

public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }
}
