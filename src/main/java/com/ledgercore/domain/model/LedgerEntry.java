package com.ledgercore.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Append-only ledger entry for a single account, tied to a transfer.
 * Ordering is per account via a monotonically increasing sequence.
 */
public record LedgerEntry(
        UUID id,
        UUID transferId,
        UUID accountId,
        long sequence,
        EntryDirection direction,
        Money money,
        Instant createdAt
) {
    public LedgerEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(transferId, "transferId");
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(money, "money");
        Objects.requireNonNull(createdAt, "createdAt");

        if (sequence < 1) {
            throw new IllegalArgumentException("sequence must be >= 1, got: " + sequence);
        }
    }
}
