package com.ledgercore.domain.model;

/**
 * Direction of a ledger entry on a single account.
 * The semantics of DEBIT/CREDIT depend on account type in real systems,
 * but for this project we keep a consistent convention in balance calculation.
 */
public enum EntryDirection {
    DEBIT,
    CREDIT
}
