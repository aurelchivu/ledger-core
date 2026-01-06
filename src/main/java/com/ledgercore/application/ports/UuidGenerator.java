package com.ledgercore.application.ports;

import java.util.UUID;

/**
 * UUID abstraction for deterministic tests.
 */
public interface UuidGenerator {
    UUID randomUuid();
}
