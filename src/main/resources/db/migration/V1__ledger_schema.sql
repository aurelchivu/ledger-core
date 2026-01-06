-- V1__ledger_schema.sql
-- Purpose: Minimal double-entry transfer ledger schema
-- Key properties:
--   - append-only ledger_entries
--   - idempotency via commands + unique transfer per command
--   - deterministic per-account ordering via account_sequences
--   - atomic event publication via outbox_events (outbox pattern)

-- =========================
-- Types
-- =========================
CREATE TYPE entry_direction AS ENUM ('DEBIT', 'CREDIT');
CREATE TYPE account_status  AS ENUM ('OPEN', 'CLOSED');
CREATE TYPE outbox_status   AS ENUM ('PENDING', 'SENT', 'FAILED');

-- =========================
-- Accounts (no balance column by design)
-- =========================
CREATE TABLE accounts (
  id             UUID PRIMARY KEY,
  status         account_status NOT NULL,
  allow_negative BOOLEAN NOT NULL DEFAULT FALSE,

  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  correlation_id TEXT
);

CREATE INDEX idx_accounts_created_at ON accounts(created_at);

-- =========================
-- Commands (idempotency gate)
-- Insert-first. Conflict means retry.
-- =========================
CREATE TABLE commands (
  command_id     UUID PRIMARY KEY,
  command_type   TEXT NOT NULL,         -- always "Transfer" in this project
  correlation_id TEXT,

  status         TEXT NOT NULL,         -- RECEIVED | APPLIED | REJECTED
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  applied_at     TIMESTAMPTZ
);

CREATE INDEX idx_commands_created_at ON commands(created_at);

-- =========================
-- Transfers (business fact / aggregate record)
-- One transfer per command_id (idempotency).
-- =========================
CREATE TABLE transfers (
  id              UUID PRIMARY KEY,
  command_id      UUID NOT NULL REFERENCES commands(command_id),

  from_account_id UUID NOT NULL REFERENCES accounts(id),
  to_account_id   UUID NOT NULL REFERENCES accounts(id),

  amount_minor    BIGINT NOT NULL CHECK (amount_minor > 0),
  currency        CHAR(3) NOT NULL,

  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  -- basic sanity (prevents silly self-transfer bugs early)
  CONSTRAINT chk_transfer_distinct_accounts CHECK (from_account_id <> to_account_id)
);

CREATE UNIQUE INDEX uq_transfers_command
  ON transfers(command_id);

CREATE INDEX idx_transfers_created_at
  ON transfers(created_at);

CREATE INDEX idx_transfers_from_account
  ON transfers(from_account_id);

CREATE INDEX idx_transfers_to_account
  ON transfers(to_account_id);

-- =========================
-- Per-account sequence generator (deterministic ordering)
-- Always locked via SELECT ... FOR UPDATE.
-- =========================
CREATE TABLE account_sequences (
  account_id    UUID PRIMARY KEY REFERENCES accounts(id),
  next_sequence BIGINT NOT NULL CHECK (next_sequence >= 1)
);

-- =========================
-- Ledger entries (append-only truth)
-- Two entries per transfer:
--   - one on from_account_id
--   - one on to_account_id
-- Deterministic ordering per account via (account_id, sequence) uniqueness.
-- =========================
CREATE TABLE ledger_entries (
  id           UUID PRIMARY KEY,

  transfer_id  UUID NOT NULL REFERENCES transfers(id),
  account_id   UUID NOT NULL REFERENCES accounts(id),

  sequence     BIGINT NOT NULL CHECK (sequence >= 1),
  direction    entry_direction NOT NULL,
  amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
  currency     CHAR(3) NOT NULL,

  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Enforce deterministic ordering per account
CREATE UNIQUE INDEX uq_ledger_account_sequence
  ON ledger_entries(account_id, sequence);

-- Prevent duplicate account postings per transfer (exactly one entry per account per transfer)
CREATE UNIQUE INDEX uq_ledger_transfer_account
  ON ledger_entries(transfer_id, account_id);

CREATE INDEX idx_ledger_account_created
  ON ledger_entries(account_id, created_at);

CREATE INDEX idx_ledger_transfer
  ON ledger_entries(transfer_id);

-- =========================
-- Balance snapshots (cache, not truth)
-- Keep it minimal; recomputable from ledger_entries.
-- =========================
CREATE TABLE balance_snapshots (
  account_id     UUID PRIMARY KEY REFERENCES accounts(id),
  as_of_sequence BIGINT NOT NULL CHECK (as_of_sequence >= 0),
  balance_minor  BIGINT NOT NULL,
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =========================
-- Outbox events (atomic publication boundary)
-- Stored in the same DB transaction as ledger_entries/transfers/commands status update.
-- =========================
CREATE TABLE outbox_events (
  id             UUID PRIMARY KEY,

  aggregate_type TEXT NOT NULL,      -- "Transfer"
  aggregate_id   UUID NOT NULL,      -- transfers.id
  event_type     TEXT NOT NULL,      -- "TransferCompleted"
  payload_json   JSONB NOT NULL,

  command_id     UUID NOT NULL REFERENCES commands(command_id),
  correlation_id TEXT,

  status         outbox_status NOT NULL DEFAULT 'PENDING',
  attempts       INT NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  available_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_error     TEXT
);

CREATE INDEX idx_outbox_pending
  ON outbox_events(status, available_at);

CREATE INDEX idx_outbox_command
  ON outbox_events(command_id);

CREATE INDEX idx_outbox_created_at
  ON outbox_events(created_at);
