CREATE TABLE IF NOT EXISTS outbox_event (
    id              BIGSERIAL PRIMARY KEY,
    event_uid       UUID NOT NULL,

    aggregate_type  VARCHAR(50) NOT NULL,   -- 'job_history'
    aggregate_id    BIGINT NOT NULL,        -- job_history.id

    event_type      VARCHAR(80) NOT NULL,   -- ExportCompleted/ExportFailed
    payload_json    JSONB NOT NULL,

    status          VARCHAR(20) NOT NULL DEFAULT 'NEW',
    attempt_count   INTEGER NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMPTZ,
    last_error      TEXT,

    sent_at         TIMESTAMPTZ,
    creation_time   TIMESTAMPTZ NOT NULL,
    update_time     TIMESTAMPTZ NOT NULL,
    creation_user   VARCHAR(100) NOT NULL,
    update_user     VARCHAR(100) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_outbox_event_uid ON outbox_event(event_uid);
CREATE INDEX IF NOT EXISTS idx_outbox_due ON outbox_event(status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_outbox_agg ON outbox_event(aggregate_type, aggregate_id);
