# Ledger Core - Design Notes

## Purpose

This project explores how to **move monetary value between accounts safely**.

The focus is not on APIs, scalability, or infrastructure, but on **correctness under failure**:
- atomic transfers,
- conservation of value,
- idempotency under retries,
- deterministic ordering,
- and full auditability.

The system is intentionally small.  
Every omission is deliberate.

---

## What Problem This System Solves

The system executes **transfers between accounts** using a **double-entry ledger**.

It guarantees that:
- money is neither created nor destroyed,
- every transfer is atomic,
- every state change has a recorded cause,
- and the current state can always be reconstructed from history.

This is not a payment gateway.  
It is a **ledger core** - the part that must not lie.

---

## Core Modeling Decisions

### Double-Entry by Construction

Every transfer produces **exactly two ledger entries**:
- a debit entry on one account,
- a credit entry on another account.

Invariant:

> **The sum of all entries per transfer is always zero.**

There is no such thing as a “single-sided” monetary operation.

**Why**  
Single-entry systems hide inconsistencies.  
Double-entry systems make them impossible to ignore.

---

### Balances Are Derived, Not Owned

Accounts do not store mutable balances.

Instead:
- transfers append ledger entries,
- balances are derived from ordered history,
- optional snapshots exist only as caches, not truth.

**Why**  
Derived state is slower to compute but easier to trust under partial failure and replay.

---

### Transfers Are the Only State-Changing Command

The system supports exactly one state-changing operation:



There are no “debits”, “credits”, or “adjustments” as standalone operations.

**Why**  
Fewer commands --> fewer invariants --> fewer failure modes.

---

## Command Handling & Idempotency

All state changes are initiated via explicit commands.

Each command:
- carries a globally unique `commandId`,
- is claimed exactly once,
- is safe to retry indefinitely.

Reprocessing the same command:
- does not duplicate ledger entries,
- returns the original result.

**Why**  
Retries are not exceptional - they are expected.

---

## Transactional Guarantees

### Atomic Transfers

For a transfer to succeed, all of the following must commit together**:
- both ledger entries,
- updated balance snapshots (if present),
- the outbox event,
- the command status.

If any step fails, **nothing is committed**.

**Why**  
Partial success is indistinguishable from corruption in financial systems.

---

### Deterministic Ordering

Ledger entries are strictly ordered **per account**.

Balance derivation depends only on this ordering.

**Why**  
Without deterministic order, balances become opinions.

---

### No Negative Balances (By Default)

Accounts reject transfers that would result in a negative balance,
unless explicitly configured otherwise.

**Why**  
Violations must be explicit decisions, not accidental outcomes.

---

## Event Emission & Outbox Pattern

Transfers emit immutable domain events (e.g. `TransferCompleted`).

Events are written to an **outbox table in the same database transaction** as the ledger entries.

A separate publisher process delivers events to external consumers.

**Why**
- events must never be published for uncommitted state,
- publishing must be retryable without duplicating effects.

Events are treated as **facts**, not instructions.

---

## Auditability & Reconstruction

The system is designed to answer one primary question:

> **“Why does this account have this balance?”**

To support this:
- every ledger entry references its originating command,
- all timestamps are immutable,
- no background mutation occurs without a recorded cause,
- balances can be fully reconstructed from history.

Debugging relies on reconstruction, not guesswork.

---

## Architecture & Boundaries

The system follows a layered / hexagonal structure:
- domain logic is isolated from infrastructure,
- application services coordinate use cases,
- persistence and messaging are adapters, not authorities.

Frameworks are intentionally kept at the edges.

**Why**  
Architecture should prevent invalid states, not just handle them.

---

## Explicit Non-Goals

This project intentionally does **not** include:
- authentication or authorization,
- user interfaces,
- exchange rates or currency conversion,
- distributed transactions,
- high availability or scaling concerns,
- microservices or cloud deployment.

These concerns are orthogonal and would dilute the learning objective.

---

## Tradeoffs & Limitations

- Strict consistency limits parallelism.
- Append-only history increases storage usage.
- Double-entry requires more upfront modeling.

These tradeoffs are accepted in exchange for correctness.

---

## Status

This project is intentionally incomplete.

It evolves as design decisions are validated through implementation,
concurrency tests, and failure analysis.

---

## Closing Note

This system exists to explore **how to reason about state transitions under failure**.

If the design feels boring, that is intentional.

Correct systems usually are.

---

