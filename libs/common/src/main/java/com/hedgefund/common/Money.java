package com.hedgefund.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Simple value object for money - demonstrates Java 21 record.
 */
public record Money(BigDecimal amount, String currency) {

    public Money {
        if (amount == null || currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("amount and currency required");
        }
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        currency = currency.toUpperCase();
    }

    public static Money of(double amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
        return new Money(amount.add(other.amount), currency);
    }
}
