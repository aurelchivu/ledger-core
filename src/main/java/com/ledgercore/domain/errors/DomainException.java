package com.ledgercore.domain.errors;

/**
 * Base type for domain-level failures.
 * Domain exceptions represent violated business rules / invariants.
 */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}