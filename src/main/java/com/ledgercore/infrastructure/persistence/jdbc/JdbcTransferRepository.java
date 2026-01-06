package com.ledgercore.infrastructure.persistence.jdbc;

import com.ledgercore.application.ports.TransferRepository;
import com.ledgercore.domain.model.Transfer;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;

public final class JdbcTransferRepository implements TransferRepository {

    private final JdbcTemplate jdbc;

    public JdbcTransferRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(Transfer transfer) {
        jdbc.update("""
        INSERT INTO transfers(
          id, command_id, from_account_id, to_account_id,
          amount_minor, currency, created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
                transfer.id(),
                transfer.commandId(),
                transfer.fromAccountId(),
                transfer.toAccountId(),
                transfer.money().amountMinor(),
                transfer.money().currency().code(),
                Timestamp.from(transfer.createdAt())
        );
    }
}
