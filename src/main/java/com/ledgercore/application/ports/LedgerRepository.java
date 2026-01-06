package com.ledgercore.application.ports;

import com.ledgercore.domain.model.LedgerEntry;

public interface LedgerRepository {
    void insert(LedgerEntry entry);
}
