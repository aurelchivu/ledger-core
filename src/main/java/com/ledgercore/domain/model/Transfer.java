package com.ledgercore.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Transfer is the business fact: move value from one account to another.
 * One transfer corresponds to one commandId (idempotency).
 */
public record Transfer(
        UUID id,
        UUID commandId,
        UUID fromAccountId,
        UUID toAccountId,
        Money money,
        Instant createdAt
) {
    public Transfer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(commandId, "commandId");
        Objects.requireNonNull(fromAccountId, "fromAccountId");
        Objects.requireNonNull(toAccountId, "toAccountId");
        Objects.requireNonNull(money, "money");
        Objects.requireNonNull(createdAt, "createdAt");

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("fromAccountId must differ from toAccountId");
        }
    }
}
