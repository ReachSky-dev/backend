package pl.reachsky.backend.auction.domain;

import pl.reachsky.backend.shared.Money;

import java.util.UUID;

/**
 * Result of settling an auction.
 * AlreadySettled means the auction was already in a terminal state — idempotent no-op.
 */
public sealed interface SettlementResult {

    record Sold(UUID winnerId, Money amount) implements SettlementResult {}

    record ReserveNotMet() implements SettlementResult {}

    record AlreadySettled() implements SettlementResult {}
}
