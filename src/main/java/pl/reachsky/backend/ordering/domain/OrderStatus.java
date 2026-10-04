package pl.reachsky.backend.ordering.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    EXPIRED,
    CANCELLED
}
