package com.ledgercore.application.result;

import java.util.UUID;

/**
 * Minimal result returned by the application layer.
 * In an idempotent system, returning transferId enables retries to return the same result.
 */
public record TransferResult(
        UUID transferId,
        UUID commandId
) { }
