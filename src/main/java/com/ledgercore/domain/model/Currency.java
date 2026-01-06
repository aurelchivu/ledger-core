package com.ledgercore.domain.model;

import java.util.Objects;

/**
 * ISO-4217 currency code. For this project we keep it simple: 3-letter uppercase.
 */
public record Currency(String code) {

    public Currency {
        Objects.requireNonNull(code, "code");
        if (!code.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("Currency code must be 3 uppercase letters, got: " + code);
        }
    }

    public static Currency of(String code) {
        return new Currency(code);
    }
}