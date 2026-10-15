-- Personal Travel Wallet (SRS FR-S3-002). Items belong to one user; trip_id is
-- an optional link that survives trip deletion as NULL (wallet preservation).
CREATE TABLE wallet_items (
    id              uuid         PRIMARY KEY,
    user_id         uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    trip_id         uuid         REFERENCES trips (id) ON DELETE SET NULL,
    title           varchar(200) NOT NULL,
    item_type       varchar(30)  NOT NULL
        CHECK (item_type IN ('TICKET', 'CONFIRMATION', 'ID')),
    storage_key     varchar(500) NOT NULL UNIQUE,
    file_name       varchar(200) NOT NULL,
    content_type    varchar(100) NOT NULL,
    file_size_bytes bigint       NOT NULL,
    is_shared       boolean      NOT NULL DEFAULT false,
    -- "shared" requires a trip; enforced in WalletService rather than a CHECK,
    -- because ON DELETE SET NULL on trip_id would then violate it.
    created_at      timestamp(6) with time zone NOT NULL
);

CREATE INDEX idx_wallet_items_user ON wallet_items (user_id, created_at DESC);
CREATE INDEX idx_wallet_items_trip ON wallet_items (trip_id) WHERE is_shared;

-- One metadata row per stored object (SRS: s3_key UNIQUE).
ALTER TABLE documents ADD CONSTRAINT documents_storage_key_key UNIQUE (storage_key);
ALTER TABLE trip_media ADD CONSTRAINT trip_media_storage_key_key UNIQUE (storage_key);

-- Idempotency keys now live in Redis with a 24h TTL (SRS FR-EXP-002).
DROP TABLE idempotency_keys;
