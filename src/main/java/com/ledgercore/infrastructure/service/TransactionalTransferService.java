package com.ledgercore.infrastructure.service;

import com.ledgercore.application.command.TransferCommand;
import com.ledgercore.application.result.TransferResult;
import com.ledgercore.application.service.TransferHandler;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * IMPORTANT: This is where Spring transactions live.
 * The application handler stays pure.
 */
public final class TransactionalTransferService implements TransferService {

    private final TransferHandler handler;

    public TransactionalTransferService(TransferHandler handler) {
        this.handler = Objects.requireNonNull(handler, "handler");
    }

    @Override
    @Transactional
    public TransferResult transfer(TransferCommand command) {
        return handler.handle(command);
    }
}
