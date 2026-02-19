CREATE TABLE IF NOT EXISTS outbox_delivery (
    id              BIGSERIAL PRIMARY KEY,

    outbox_event_id BIGINT NOT NULL,
    target_id       BIGINT NOT NULL,

    status          VARCHAR(20) NOT NULL DEFAULT 'NEW', -- NEW, CLAIMED, DELIVERED, DEAD
    attempt_count   INTEGER NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMPTZ,
    last_error      TEXT,

    --claim/lease
    claimed_by      VARCHAR(100),
    claimed_until   TIMESTAMPTZ,

    delivered_at    TIMESTAMPTZ,

    creation_time   TIMESTAMPTZ NOT NULL,
    update_time     TIMESTAMPTZ NOT NULL,
    creation_user   VARCHAR(100) NOT NULL,
    update_user     VARCHAR(100) NOT NULL,

    CONSTRAINT fk_outbox_delivery_event
    FOREIGN KEY (outbox_event_id) REFERENCES outbox_event(id) ON DELETE CASCADE,
    CONSTRAINT fk_outbox_delivery_target
    FOREIGN KEY (target_id) REFERENCES delivery_target(id) ON DELETE CASCADE,

    -- one event for target will only have one delivery record here
    CONSTRAINT uq_outbox_delivery_event_target UNIQUE (outbox_event_id, target_id)
    );

CREATE INDEX IF NOT EXISTS idx_outbox_delivery_due
    ON outbox_delivery(status, next_retry_at);

-- target -> push / pull
CREATE INDEX IF NOT EXISTS idx_outbox_delivery_target_status_due
    ON outbox_delivery(target_id, status, next_retry_at);

-- claim timeout
CREATE INDEX IF NOT EXISTS idx_outbox_delivery_claim_expiry
    ON outbox_delivery(status, claimed_until);

-- ACK/NACK by claimed_by
CREATE INDEX IF NOT EXISTS idx_outbox_delivery_claimed_by
    ON outbox_delivery(claimed_by, status);