package com.ledgercore.domain.policy;

import com.ledgercore.domain.errors.InsufficientFundsException;
import com.ledgercore.domain.model.Account;
import com.ledgercore.domain.model.EntryDirection;
import com.ledgercore.domain.model.Money;

import java.util.Objects;

/**
 * Encapsulates rules about balances (e.g., no negative balance).
 * This keeps "how we interpret balance" out of the handler.
 *
 * Convention in this project:
 *  - CREDIT increases balance
 *  - DEBIT decreases balance
 *
 * (This is a simplification; real accounting depends on account type.)
 */
public final class BalancePolicy {

    /**
     * @param currentBalanceMinor current derived balance in minor units
     * @param entryDirection direction of the entry to apply
     * @param amount money amount (minor units)
     * @return new balance after applying the entry
     */
    public long apply(Account account, long currentBalanceMinor, EntryDirection entryDirection, Money amount) {
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(entryDirection, "entryDirection");
        Objects.requireNonNull(amount, "amount");

        long delta = switch (entryDirection) {
            case CREDIT -> amount.amountMinor();
            case DEBIT -> -amount.amountMinor();
        };

        long next = currentBalanceMinor + delta;

        if (!account.allowNegative() && next < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds: current=" + currentBalanceMinor + ", delta=" + delta + ", next=" + next
            );
        }

        return next;
    }
}
