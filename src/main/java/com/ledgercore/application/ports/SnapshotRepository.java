package com.ledgercore.application.ports;

import java.util.UUID;

/**
 * Balance snapshots are a cache. We keep their API small:
 * - read current cached balance (or 0 if missing)
 * - upsert new snapshot for a given account at a given sequence
 */
public interface SnapshotRepository {

    long currentBalanceMinor(UUID accountId);

    void upsert(UUID accountId, long asOfSequence, long balanceMinor);
}
