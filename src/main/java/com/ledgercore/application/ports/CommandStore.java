package com.ledgercore.application.ports;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Idempotency gate: claim a command exactly once.
 * Implementation will use the commands table.
 */
public interface CommandStore {

    /**
     * Attempts to insert a new command (RECEIVED).
     * @return true if inserted (first time), false if it already exists (retry).
     */
    boolean tryInsertReceived(UUID commandId, String commandType, String correlationId, Instant now);

    /**
     * Marks command as APPLIED.
     */
    void markApplied(UUID commandId, Instant appliedAt);

    /**
     * If already processed, return the previously associated transferId (from transfers table).
     * This allows retries to return the original result.
     */
    Optional<UUID> findTransferIdByCommandId(UUID commandId);
}
