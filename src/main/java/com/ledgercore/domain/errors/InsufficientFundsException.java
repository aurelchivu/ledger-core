package com.ledgercore.domain.errors;

/**
 * Thrown when an operation would violate a "no negative balance" rule.
 */
public final class InsufficientFundsException extends DomainException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}