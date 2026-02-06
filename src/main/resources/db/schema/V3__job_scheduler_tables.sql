-- PostgreSQL

CREATE TABLE IF NOT EXISTS job (
    id              BIGSERIAL PRIMARY KEY,

    name            VARCHAR(200) NOT NULL,
    job_type        VARCHAR(80)  NOT NULL,

    enabled         BOOLEAN NOT NULL DEFAULT TRUE,

    client_id       INTEGER NOT NULL,

    --  like "0 0 2 * * *"
    cron            VARCHAR(100) NOT NULL,

    params_json     JSONB NOT NULL DEFAULT '{}'::jsonb,

    next_run_at     TIMESTAMPTZ NOT NULL,

    creation_time   TIMESTAMPTZ NOT NULL,
    update_time     TIMESTAMPTZ NOT NULL,
    creation_user   VARCHAR(100) NOT NULL,
    update_user     VARCHAR(100) NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_job_due
    ON job (enabled, next_run_at);

CREATE INDEX IF NOT EXISTS idx_job_client
    ON job (client_id);

CREATE TABLE IF NOT EXISTS job_history (
    id              BIGSERIAL PRIMARY KEY,

    job_id          BIGINT NOT NULL REFERENCES job(id) ON DELETE CASCADE,

    scheduled_time  TIMESTAMPTZ NOT NULL,
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,

    status          VARCHAR(20) NOT NULL,
    error_message   TEXT,

    -- result payload
    result_json     JSONB NOT NULL DEFAULT '{}'::jsonb,

    creation_time   TIMESTAMPTZ NOT NULL,
    update_time     TIMESTAMPTZ NOT NULL,
    creation_user   VARCHAR(100) NOT NULL,
    update_user     VARCHAR(100) NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_job_history_job_time
    ON job_history (job_id, creation_time DESC);

CREATE INDEX idx_job_history_started_at
    ON job_history (job_id, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_job_history_status
    ON job_history (status);

