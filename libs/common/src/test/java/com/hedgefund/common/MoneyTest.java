package com.hedgefund.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void addSameCurrency() {
        var a = Money.of(100.50, "USD");
        var b = Money.of(49.50, "USD");
        assertEquals(Money.of(150.00, "USD"), a.add(b));
    }

    @Test
    void currencyMismatchThrows() {
        var usd = Money.of(10, "USD");
        var eur = Money.of(10, "EUR");
        assertThrows(IllegalArgumentException.class, () -> usd.add(eur));
    }
}
