package com.ledgercore.application.ports;

import java.util.UUID;

/**
 * Outbox repository for atomic event recording.
 * Publishing happens elsewhere; this is only the write side.
 */
public interface OutboxRepository {
    void insertTransferCompleted(UUID eventId, UUID transferId, UUID commandId, String correlationId, String payloadJson);
}
