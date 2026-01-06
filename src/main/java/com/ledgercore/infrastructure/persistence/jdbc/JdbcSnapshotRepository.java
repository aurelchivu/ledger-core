package com.ledgercore.infrastructure.persistence.jdbc;

import com.ledgercore.application.ports.SnapshotRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

public final class JdbcSnapshotRepository implements SnapshotRepository {

    private final JdbcTemplate jdbc;

    public JdbcSnapshotRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long currentBalanceMinor(UUID accountId) {
        Long balance = jdbc.query("""
            SELECT balance_minor
            FROM balance_snapshots
            WHERE account_id = ?
            """,
                ps -> ps.setObject(1, accountId),
                rs -> rs.next() ? rs.getLong("balance_minor") : null
        );
        return balance == null ? 0L : balance;
    }

    @Override
    public void upsert(UUID accountId, long asOfSequence, long balanceMinor) {
        jdbc.update("""
        INSERT INTO balance_snapshots(account_id, as_of_sequence, balance_minor, updated_at)
        VALUES (?, ?, ?, NOW())
        ON CONFLICT (account_id)
        DO UPDATE SET as_of_sequence = EXCLUDED.as_of_sequence,
                      balance_minor  = EXCLUDED.balance_minor,
                      updated_at     = NOW()
        """,
                accountId, asOfSequence, balanceMinor
        );
    }
}
