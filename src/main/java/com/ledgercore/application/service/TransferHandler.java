package com.ledgercore.application.service;

import com.ledgercore.application.command.TransferCommand;
import com.ledgercore.application.ports.*;
import com.ledgercore.application.result.TransferResult;
import com.ledgercore.domain.model.*;
import com.ledgercore.domain.policy.BalancePolicy;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure application use-case: executes a transfer.
 *
 * IMPORTANT:
 * - No Spring annotations here.
 * - Assumes it's executed inside a transaction (provided by infrastructure).
 */
public final class TransferHandler {

    private final CommandStore commandStore;
    private final AccountRepository accountRepository;
    private final AccountSequenceStore accountSequenceStore;
    private final TransferRepository transferRepository;
    private final LedgerRepository ledgerRepository;
    private final SnapshotRepository snapshotRepository;
    private final OutboxRepository outboxRepository;
    private final BalancePolicy balancePolicy;
    private final Clock clock;
    private final UuidGenerator uuidGenerator;

    public TransferHandler(
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
        this.commandStore = Objects.requireNonNull(commandStore, "commandStore");
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository");
        this.accountSequenceStore = Objects.requireNonNull(accountSequenceStore, "accountSequenceStore");
        this.transferRepository = Objects.requireNonNull(transferRepository, "transferRepository");
        this.ledgerRepository = Objects.requireNonNull(ledgerRepository, "ledgerRepository");
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository, "snapshotRepository");
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository");
        this.balancePolicy = Objects.requireNonNull(balancePolicy, "balancePolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.uuidGenerator = Objects.requireNonNull(uuidGenerator, "uuidGenerator");
    }

    public TransferResult handle(TransferCommand cmd) {
        Objects.requireNonNull(cmd, "cmd");

        Instant now = clock.now();

        // 1) Idempotency gate: insert command as RECEIVED
        boolean inserted = commandStore.tryInsertReceived(cmd.commandId(), "Transfer", cmd.correlationId(), now);
        if (!inserted) {
            // Already seen. Return the original result if present.
            return commandStore.findTransferIdByCommandId(cmd.commandId())
                    .map(transferId -> new TransferResult(transferId, cmd.commandId()))
                    // If command exists but transferId missing, that's inconsistent state.
                    // We fail hard here; infrastructure/tests should catch it.
                    .orElseThrow(() -> new IllegalStateException(
                            "Command exists but no transfer found for commandId=" + cmd.commandId()
                    ));
        }

        // 2) Validate accounts exist and are open (minimal checks)
        Account from = accountRepository.findById(cmd.fromAccountId())
                .orElseThrow(() -> new IllegalArgumentException("fromAccount not found: " + cmd.fromAccountId()));
        Account to = accountRepository.findById(cmd.toAccountId())
                .orElseThrow(() -> new IllegalArgumentException("toAccount not found: " + cmd.toAccountId()));

        if (!from.isOpen() || !to.isOpen()) {
            throw new IllegalArgumentException("Both accounts must be OPEN to transfer");
        }

        // 3) Create transfer aggregate record
        UUID transferId = uuidGenerator.randomUuid();
        Transfer transfer = new Transfer(
                transferId,
                cmd.commandId(),
                cmd.fromAccountId(),
                cmd.toAccountId(),
                cmd.money(),
                now
        );
        transferRepository.insert(transfer);

        // 4) Reserve deterministic sequences (must be locked by implementation)
        long fromSeq = accountSequenceStore.nextSequenceForUpdate(from.id());
        long toSeq = accountSequenceStore.nextSequenceForUpdate(to.id());

        // 5) Enforce no-negative (default) using snapshots as current cached state
        long fromBalance = snapshotRepository.currentBalanceMinor(from.id());
        // From account is decreased by transfer amount => DEBIT (per our convention)
        long fromNext = balancePolicy.apply(from, fromBalance, EntryDirection.DEBIT, cmd.money());

        long toBalance = snapshotRepository.currentBalanceMinor(to.id());
        // To account is increased => CREDIT
        long toNext = balancePolicy.apply(to, toBalance, EntryDirection.CREDIT, cmd.money());

        // 6) Append ledger entries (two entries per transfer)
        LedgerEntry fromEntry = new LedgerEntry(
                uuidGenerator.randomUuid(),
                transferId,
                from.id(),
                fromSeq,
                EntryDirection.DEBIT,
                cmd.money(),
                now
        );

        LedgerEntry toEntry = new LedgerEntry(
                uuidGenerator.randomUuid(),
                transferId,
                to.id(),
                toSeq,
                EntryDirection.CREDIT,
                cmd.money(),
                now
        );

        ledgerRepository.insert(fromEntry);
        ledgerRepository.insert(toEntry);

        // 7) Update snapshots (cache)
        snapshotRepository.upsert(from.id(), fromSeq, fromNext);
        snapshotRepository.upsert(to.id(), toSeq, toNext);

        // 8) Record outbox event (atomic with the above changes)
        UUID eventId = uuidGenerator.randomUuid();
        String payloadJson = "{\"transferId\":\"" + transferId + "\",\"commandId\":\"" + cmd.commandId() + "\"}";
        outboxRepository.insertTransferCompleted(eventId, transferId, cmd.commandId(), cmd.correlationId(), payloadJson);

        // 9) Mark command applied
        commandStore.markApplied(cmd.commandId(), now);

        return new TransferResult(transferId, cmd.commandId());
    }
}
