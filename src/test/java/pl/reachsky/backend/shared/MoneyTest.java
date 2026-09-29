package pl.reachsky.backend.shared;

import org.junit.jupiter.api.Test;

import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency PLN = Currency.getInstance("PLN");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void negativeAmountIsRejected() {
        assertThatThrownBy(() -> new Money(-1, PLN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addReturnsSumInSameCurrency() {
        Money a = new Money(100, PLN);
        Money b = new Money(250, PLN);
        assertThat(a.add(b)).isEqualTo(new Money(350, PLN));
    }

    @Test
    void subtractReturnsRemainder() {
        Money a = new Money(300, PLN);
        Money b = new Money(100, PLN);
        assertThat(a.subtract(b)).isEqualTo(new Money(200, PLN));
    }

    @Test
    void subtractBelowZeroThrows() {
        Money a = new Money(50, PLN);
        Money b = new Money(100, PLN);
        assertThatThrownBy(() -> a.subtract(b))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addMixedCurrenciesThrows() {
        Money a = new Money(100, PLN);
        Money b = new Money(100, EUR);
        assertThatThrownBy(() -> a.add(b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency mismatch");
    }

    @Test
    void isLessThanComparesAmounts() {
        Money small = new Money(10, PLN);
        Money large = new Money(20, PLN);
        assertThat(small.isLessThan(large)).isTrue();
        assertThat(large.isLessThan(small)).isFalse();
    }

    @Test
    void isZeroReturnsTrueForZeroAmount() {
        assertThat(new Money(0, PLN).isZero()).isTrue();
        assertThat(new Money(1, PLN).isZero()).isFalse();
    }
}
