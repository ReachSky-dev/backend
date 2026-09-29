package pl.reachsky.backend.shared;

import java.util.Currency;
import java.util.Objects;

/**
 * Immutable monetary value. Amount is stored in minor units (grosze, centy, etc.)
 * to avoid floating-point errors.
 */
public record Money(long amountInMinorUnits, Currency currency) {

    public Money {
        if (amountInMinorUnits < 0) {
            throw new IllegalArgumentException("amount must be non-negative, got: " + amountInMinorUnits);
        }
        Objects.requireNonNull(currency, "currency must not be null");
    }

    public static Money of(long amountInMinorUnits, String currencyCode) {
        return new Money(amountInMinorUnits, Currency.getInstance(currencyCode));
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amountInMinorUnits + other.amountInMinorUnits, currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        long result = this.amountInMinorUnits - other.amountInMinorUnits;
        if (result < 0) {
            throw new IllegalArgumentException("subtraction would produce negative amount");
        }
        return new Money(result, currency);
    }

    public boolean isLessThan(Money other) {
        requireSameCurrency(other);
        return this.amountInMinorUnits < other.amountInMinorUnits;
    }

    public boolean isZero() {
        return amountInMinorUnits == 0;
    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }
}
