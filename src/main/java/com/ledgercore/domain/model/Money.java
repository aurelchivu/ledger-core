package com.ledgercore.domain.model;

import java.util.Objects;

/**
 * Money in minor units (e.g., cents) + currency.
 * We model minor units to avoid floating point errors.
 */
public record Money(long amountMinor, Currency currency) {

    public Money {
        Objects.requireNonNull(currency, "currency");
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Money amount must be > 0 minor units, got: " + amountMinor);
        }
    }

    public static Money ofMinor(long amountMinor, Currency currency) {
        return new Money(amountMinor, currency);
    }
}
