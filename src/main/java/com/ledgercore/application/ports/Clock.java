package com.ledgercore.application.ports;

import java.time.Instant;

/**
 * Time abstraction for testability.
 */
public interface Clock {
    Instant now();
}
