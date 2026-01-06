package com.ledgercore.infrastructure.outbox;

import com.ledgercore.application.ports.OutboxRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

public final class JdbcOutboxRepository implements OutboxRepository {

    private final JdbcTemplate jdbc;

    public JdbcOutboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insertTransferCompleted(
            UUID eventId,
            UUID transferId,
            UUID commandId,
            String correlationId,
            String payloadJson
    ) {
        jdbc.update("""
        INSERT INTO outbox_events(
          id,
          aggregate_type, aggregate_id,
          event_type, payload_json,
          command_id, correlation_id,
          status, attempts, available_at,
          created_at, last_error
        )
        VALUES (?, 'Transfer', ?, 'TransferCompleted', CAST(? AS JSONB), ?, ?, 'PENDING', 0, NOW(), NOW(), NULL)
        """,
                eventId,
                transferId,
                payloadJson,
                commandId,
                correlationId
        );
    }
}
