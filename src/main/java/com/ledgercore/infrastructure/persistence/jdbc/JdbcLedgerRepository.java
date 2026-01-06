package com.ledgercore.infrastructure.persistence.jdbc;

import com.ledgercore.application.ports.LedgerRepository;
import com.ledgercore.domain.model.LedgerEntry;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;

public final class JdbcLedgerRepository implements LedgerRepository {

    private final JdbcTemplate jdbc;

    public JdbcLedgerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(LedgerEntry entry) {
        jdbc.update("""
        INSERT INTO ledger_entries(
          id, transfer_id, account_id,
          sequence, direction, amount_minor, currency, created_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
                entry.id(),
                entry.transferId(),
                entry.accountId(),
                entry.sequence(),
                entry.direction().name(),
                entry.money().amountMinor(),
                entry.money().currency().code(),
                Timestamp.from(entry.createdAt())
        );
    }
}
