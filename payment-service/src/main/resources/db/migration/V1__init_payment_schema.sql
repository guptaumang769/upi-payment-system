-- Payments. idempotency_key is unique so a replayed request maps back to the same payment.
CREATE TABLE payments (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_ref     VARCHAR(255)   NOT NULL UNIQUE,
    from_vpa        VARCHAR(255)   NOT NULL,
    to_vpa          VARCHAR(255)   NOT NULL,
    amount          NUMERIC(19, 2) NOT NULL,
    status          VARCHAR(32)    NOT NULL,
    idempotency_key VARCHAR(255)   NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL
);

-- Outbox: events are written here in the same tx as the payment, then relayed to Kafka by the
-- OutboxPoller. Partial index keeps the poller's "find unpublished" scan cheap.
CREATE TABLE outbox_events (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type   VARCHAR(255) NOT NULL,
    payload      TEXT         NOT NULL,
    published    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_outbox_unpublished ON outbox_events (created_at) WHERE published = FALSE;
