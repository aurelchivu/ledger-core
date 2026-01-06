package com.ledgercore.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Account is intentionally minimal.
 * No stored balance. Balance is derived from ledger history (plus optional snapshots).
 */
public final class Account {
    private final UUID id;
    private final AccountStatus status;
    private final boolean allowNegative;

    public Account(UUID id, AccountStatus status, boolean allowNegative) {
        this.id = Objects.requireNonNull(id, "id");
        this.status = Objects.requireNonNull(status, "status");
        this.allowNegative = allowNegative;
    }

    public UUID id() {
        return id;
    }

    public AccountStatus status() {
        return status;
    }

    public boolean allowNegative() {
        return allowNegative;
    }

    public boolean isOpen() {
        return status == AccountStatus.OPEN;
    }
}
