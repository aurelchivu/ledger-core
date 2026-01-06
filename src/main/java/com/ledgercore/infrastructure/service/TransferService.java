package com.ledgercore.infrastructure.service;

import com.ledgercore.application.command.TransferCommand;
import com.ledgercore.application.result.TransferResult;

public interface TransferService {
    TransferResult transfer(TransferCommand command);
}
