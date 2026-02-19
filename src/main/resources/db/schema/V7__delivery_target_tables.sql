CREATE TABLE IF NOT EXISTS delivery_target (
    id           BIGSERIAL PRIMARY KEY,
    target_key      VARCHAR(100) NOT NULL,
    client_id    INTEGER NOT NULL,
    type         VARCHAR(20) NOT NULL,        -- WEBHOOK/SFTP/KAFKA
    enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    config_json  JSONB NOT NULL DEFAULT '{}'::jsonb,

    creation_time   TIMESTAMPTZ NOT NULL,
    update_time     TIMESTAMPTZ NOT NULL,
    creation_user   VARCHAR(100) NOT NULL,
    update_user     VARCHAR(100) NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_delivery_target_key ON delivery_target(target_key);
CREATE INDEX IF NOT EXISTS idx_delivery_target_client ON delivery_target(client_id, enabled);
CREATE INDEX IF NOT EXISTS idx_delivery_target_type_enabled ON delivery_target(type, enabled);

