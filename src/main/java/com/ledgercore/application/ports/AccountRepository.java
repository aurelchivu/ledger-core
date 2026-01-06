package com.ledgercore.application.ports;

import com.ledgercore.domain.model.Account;

import java.util.Optional;
import java.util.UUID;

/**
 * Read access to accounts. Keep it minimal for now.
 */
public interface AccountRepository {
    Optional<Account> findById(UUID accountId);
}
