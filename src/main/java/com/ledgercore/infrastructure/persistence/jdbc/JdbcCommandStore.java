package com.ledgercore.infrastructure.persistence.jdbc;

import com.ledgercore.application.ports.CommandStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class JdbcCommandStore implements CommandStore {

    private final JdbcTemplate jdbc;

    public JdbcCommandStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean tryInsertReceived(UUID commandId, String commandType, String correlationId, Instant now) {
        // NOTE: "insert-first" idempotency gate. Conflict => already seen.
        int rows = jdbc.update("""
        INSERT INTO commands(command_id, command_type, correlation_id, status, created_at)
        VALUES (?, ?, ?, 'RECEIVED', ?)
        ON CONFLICT (command_id) DO NOTHING
        """,
                commandId,
                commandType,
                correlationId,
                Timestamp.from(now)
        );
        return rows == 1;
    }

    @Override
    public void markApplied(UUID commandId, Instant appliedAt) {
        jdbc.update("""
        UPDATE commands
        SET status = 'APPLIED', applied_at = ?
        WHERE command_id = ?
        """,
                Timestamp.from(appliedAt),
                commandId
        );
    }

    @Override
    public Optional<UUID> findTransferIdByCommandId(UUID commandId) {
        // NOTE: transfers(command_id) is unique in schema.
        return jdbc.query("""
            SELECT id FROM transfers WHERE command_id = ?
            """,
                ps -> ps.setObject(1, commandId),
                rs -> rs.next() ? Optional.of((UUID) rs.getObject("id")) : Optional.empty()
        );
    }
}
