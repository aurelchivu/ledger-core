package com.ledgercore.infrastructure.persistence.jdbc;

import com.ledgercore.application.ports.AccountSequenceStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

public final class JdbcAccountSequenceStore implements AccountSequenceStore {

    private final JdbcTemplate jdbc;

    public JdbcAccountSequenceStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long nextSequenceForUpdate(UUID accountId) {
        // IMPORTANT: This MUST run inside a DB transaction.
        // We lock the row to guarantee deterministic ordering under concurrency.
        Long current = jdbc.query("""
            SELECT next_sequence
            FROM account_sequences
            WHERE account_id = ?
            FOR UPDATE
            """,
                ps -> ps.setObject(1, accountId),
                rs -> rs.next() ? rs.getLong("next_sequence") : null
        );

        if (current == null) {
            throw new IllegalStateException("Missing account_sequences row for account_id=" + accountId);
        }

        jdbc.update("""
        UPDATE account_sequences
        SET next_sequence = next_sequence + 1
        WHERE account_id = ?
        """, accountId);

        return current;
    }
}
