-- Wallets: one row per VPA. version column backs JPA optimistic locking on the balance.
CREATE TABLE wallets (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vpa      VARCHAR(255)   NOT NULL UNIQUE,
    balance  NUMERIC(19, 2) NOT NULL DEFAULT 0,
    version  BIGINT         NOT NULL DEFAULT 0
);

-- Double-entry ledger: append-only history of every balance change.
CREATE TABLE ledger_entries (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    wallet_id     BIGINT         NOT NULL REFERENCES wallets (id),
    type          VARCHAR(16)    NOT NULL,
    amount        NUMERIC(19, 2) NOT NULL,
    balance_after NUMERIC(19, 2) NOT NULL,
    payment_id    VARCHAR(255)   NOT NULL,
    created_at    TIMESTAMPTZ    NOT NULL
);

-- Idempotency guard: one debit AND one credit at most per payment (a self-transfer could have both).
CREATE UNIQUE INDEX ux_ledger_payment_type ON ledger_entries (payment_id, type);
CREATE INDEX ix_ledger_wallet ON ledger_entries (wallet_id);
