package com.ledgercore.infrastructure.config;

import com.ledgercore.application.ports.*;
import com.ledgercore.application.service.TransferHandler;
import com.ledgercore.domain.policy.BalancePolicy;
import com.ledgercore.infrastructure.outbox.JdbcOutboxRepository;
import com.ledgercore.infrastructure.persistence.jdbc.*;
import com.ledgercore.infrastructure.service.TransferService;
import com.ledgercore.infrastructure.service.TransactionalTransferService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

@Configuration
public class AppConfig {

    // ---------- Pure domain policy ----------
    @Bean
    BalancePolicy balancePolicy() {
        return new BalancePolicy();
    }

    // ---------- Ports implementations (JDBC adapters) ----------
    @Bean
    CommandStore commandStore(JdbcTemplate jdbc) {
        return new JdbcCommandStore(jdbc);
    }

    @Bean
    AccountRepository accountRepository(JdbcTemplate jdbc) {
        return new JdbcAccountRepository(jdbc);
    }

    @Bean
    AccountSequenceStore accountSequenceStore(JdbcTemplate jdbc) {
        return new JdbcAccountSequenceStore(jdbc);
    }

    @Bean
    TransferRepository transferRepository(JdbcTemplate jdbc) {
        return new JdbcTransferRepository(jdbc);
    }

    @Bean
    LedgerRepository ledgerRepository(JdbcTemplate jdbc) {
        return new JdbcLedgerRepository(jdbc);
    }

    @Bean
    SnapshotRepository snapshotRepository(JdbcTemplate jdbc) {
        return new JdbcSnapshotRepository(jdbc);
    }

    @Bean
    OutboxRepository outboxRepository(JdbcTemplate jdbc) {
        return new JdbcOutboxRepository(jdbc);
    }

    // ---------- Pure utilities as ports ----------
    @Bean
    Clock clock() {
        return Instant::now;
    }

    @Bean
    UuidGenerator uuidGenerator() {
        return UUID::randomUUID;
    }

    // ---------- Application handler (pure) ----------
    @Bean
    TransferHandler transferHandler(
            CommandStore commandStore,
            AccountRepository accountRepository,
            AccountSequenceStore accountSequenceStore,
            TransferRepository transferRepository,
            LedgerRepository ledgerRepository,
            SnapshotRepository snapshotRepository,
            OutboxRepository outboxRepository,
            BalancePolicy balancePolicy,
            Clock clock,
            UuidGenerator uuidGenerator
    ) {
        return new TransferHandler(
                commandStore,
                accountRepository,
                accountSequenceStore,
                transferRepository,
                ledgerRepository,
                snapshotRepository,
                outboxRepository,
                balancePolicy,
                clock,
                uuidGenerator
        );
    }

    // ---------- Transaction boundary (Spring) ----------
    @Bean
    TransferService transferService(TransferHandler handler) {
        return new TransactionalTransferService(handler);
    }
}
