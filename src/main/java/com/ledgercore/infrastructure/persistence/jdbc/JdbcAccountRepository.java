package com.ledgercore.infrastructure.persistence.jdbc;

import com.ledgercore.application.ports.AccountRepository;
import com.ledgercore.domain.model.Account;
import com.ledgercore.domain.model.AccountStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.UUID;

public final class JdbcAccountRepository implements AccountRepository {

    private final JdbcTemplate jdbc;

    public JdbcAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Account> findById(UUID accountId) {
        return jdbc.query("""
            SELECT id, status, allow_negative
            FROM accounts
            WHERE id = ?
            """,
                ps -> ps.setObject(1, accountId),
                rs -> {
                    if (!rs.next()) return Optional.empty();
                    UUID id = (UUID) rs.getObject("id");
                    AccountStatus status = AccountStatus.valueOf(rs.getString("status"));
                    boolean allowNegative = rs.getBoolean("allow_negative");
                    return Optional.of(new Account(id, status, allowNegative));
                }
        );
    }
}
