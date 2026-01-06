package com.ledgercore.application.ports;

import com.ledgercore.domain.model.Transfer;

public interface TransferRepository {
    void insert(Transfer transfer);
}
