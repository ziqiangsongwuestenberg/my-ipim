-- PostgreSQL
CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- for gen_random_uuid()

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

CREATE INDEX IF NOT EXISTS idx_job_client_type
    ON job (client_id, job_type);

CREATE TABLE IF NOT EXISTS job_history (
    id              BIGSERIAL PRIMARY KEY,

    job_id          BIGINT NOT NULL REFERENCES job(id) ON DELETE CASCADE,

    -- stable external identifier for one run (great for tracing / webhook idempotency)
    run_uid         UUID NOT NULL DEFAULT gen_random_uuid(),

    scheduled_time  TIMESTAMPTZ NOT NULL,
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,

    status          VARCHAR(20) NOT NULL,
    error_message   TEXT,

    -- exported artifact info
    artifact_uri     TEXT,          -- e.g. s3://bucket/key or key
    checksum_sha256  VARCHAR(64),    -- sha256 hex
    size_bytes       BIGINT,

    output_format    VARCHAR(20),    -- XML/JSON/CSV
    schema_version   VARCHAR(50),

    -- result payload
    result_json     JSONB NOT NULL DEFAULT '{}'::jsonb,

    creation_time   TIMESTAMPTZ NOT NULL,
    update_time     TIMESTAMPTZ NOT NULL,
    creation_user   VARCHAR(100) NOT NULL,
    update_user     VARCHAR(100) NOT NULL
    );


-- indexes
CREATE UNIQUE INDEX IF NOT EXISTS ux_job_history_run_uid
    ON job_history (run_uid);

-- latest runs of a job
CREATE INDEX IF NOT EXISTS idx_job_history_job_creation_time
    ON job_history (job_id, creation_time DESC);

-- latest started runs of a job
CREATE INDEX IF NOT EXISTS idx_job_history_job_started_at
    ON job_history (job_id, started_at DESC);

-- find by status
CREATE INDEX IF NOT EXISTS idx_job_history_status
    ON job_history (status);

-- latest finished runs of a job
CREATE INDEX IF NOT EXISTS idx_job_history_job_finished_at
    ON job_history (job_id, finished_at DESC)
    WHERE finished_at IS NOT NULL;

-- successful exports with artifacts
CREATE INDEX IF NOT EXISTS idx_job_history_job_success_artifact
    ON job_history (job_id, finished_at DESC)
    WHERE status = 'SUCCESS' AND artifact_uri IS NOT NULL;