package com.ledgercore.application.command;

import com.ledgercore.domain.model.Currency;
import com.ledgercore.domain.model.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * The only state-changing command in this project.
 * commandId is the idempotency key.
 */
public record TransferCommand(
        UUID commandId,
        UUID fromAccountId,
        UUID toAccountId,
        Money money,
        String correlationId
) {
    public TransferCommand {
        Objects.requireNonNull(commandId, "commandId");
        Objects.requireNonNull(fromAccountId, "fromAccountId");
        Objects.requireNonNull(toAccountId, "toAccountId");
        Objects.requireNonNull(money, "money");
        // correlationId can be null, but keep it visible as a field
    }

    public Currency currency() {
        return money.currency();
    }
}
