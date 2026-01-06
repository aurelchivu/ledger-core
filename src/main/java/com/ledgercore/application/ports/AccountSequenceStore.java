package com.ledgercore.application.ports;

import java.util.UUID;

/**
 * Deterministic per-account sequencing for ledger entries.
 * Implementation MUST lock rows (SELECT ... FOR UPDATE) inside an existing DB transaction.
 */
public interface AccountSequenceStore {

    /**
     * Reserves and returns the next sequence number for the account.
     * Must be concurrency-safe.
     */
    long nextSequenceForUpdate(UUID accountId);
}
