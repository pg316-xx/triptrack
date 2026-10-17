-- 1. Deleting a trip deletes everything that belongs to it (SRS 9.3).
--    Audit logs and notifications keep their history with trip_id = NULL;
--    wallet items already use ON DELETE SET NULL (V5).
--    The V1 constraints have generated names, so look them up by column.
CREATE FUNCTION pg_temp.set_fk_action(tbl text, col text, ref_tbl text, action text) RETURNS void AS $$
DECLARE
    con text;
BEGIN
    SELECT c.conname INTO con
    FROM pg_constraint c
    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
    WHERE c.contype = 'f'
      AND c.conrelid = ('public.' || tbl)::regclass
      AND a.attname = col;

    IF con IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT %I', tbl, con);
    END IF;

    EXECUTE format(
        'ALTER TABLE public.%I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES public.%I (id) ON DELETE %s',
        tbl, tbl || '_' || col || '_fkey', col, ref_tbl, action
    );
END;
$$ LANGUAGE plpgsql;

SELECT pg_temp.set_fk_action('trip_members',     'trip_id',          'trips',          'CASCADE');
SELECT pg_temp.set_fk_action('trip_invitations', 'trip_id',          'trips',          'CASCADE');
SELECT pg_temp.set_fk_action('itinerary_days',   'trip_id',          'trips',          'CASCADE');
SELECT pg_temp.set_fk_action('activities',       'itinerary_day_id', 'itinerary_days', 'CASCADE');
SELECT pg_temp.set_fk_action('expenses',         'trip_id',          'trips',          'CASCADE');
SELECT pg_temp.set_fk_action('expense_splits',   'expense_id',       'expenses',       'CASCADE');
SELECT pg_temp.set_fk_action('settlements',      'trip_id',          'trips',          'CASCADE');
SELECT pg_temp.set_fk_action('documents',        'trip_id',          'trips',          'CASCADE');
SELECT pg_temp.set_fk_action('trip_media',       'trip_id',          'trips',          'CASCADE');
SELECT pg_temp.set_fk_action('chat_messages',    'trip_id',          'trips',          'CASCADE');
SELECT pg_temp.set_fk_action('notifications',    'trip_id',          'trips',          'SET NULL');
SELECT pg_temp.set_fk_action('audit_logs',       'trip_id',          'trips',          'SET NULL');

-- Indexes for the cascades and the common lookups (SRS 9.2).
CREATE INDEX IF NOT EXISTS idx_trip_members_user        ON trip_members (user_id);
CREATE INDEX IF NOT EXISTS idx_trip_invitations_trip    ON trip_invitations (trip_id);
CREATE INDEX IF NOT EXISTS idx_itinerary_days_trip      ON itinerary_days (trip_id, day_number);
CREATE INDEX IF NOT EXISTS idx_expenses_trip_created    ON expenses (trip_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_expense_splits_expense   ON expense_splits (expense_id);
CREATE INDEX IF NOT EXISTS idx_expense_splits_user      ON expense_splits (user_id);
CREATE INDEX IF NOT EXISTS idx_settlements_trip         ON settlements (trip_id);
CREATE INDEX IF NOT EXISTS idx_documents_trip           ON documents (trip_id);
CREATE INDEX IF NOT EXISTS idx_trip_media_trip_created  ON trip_media (trip_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_trip       ON chat_messages (trip_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read  ON notifications (user_id, is_read, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_trip_created  ON audit_logs (trip_id, created_at DESC);

-- 2. Trip cover image (SRS FR-TRIP-001).
ALTER TABLE trips ADD COLUMN cover_image_url varchar(1000);

-- 3. One-time password reset links (only the token hash is stored).
CREATE TABLE password_reset_tokens (
    id          uuid         PRIMARY KEY,
    user_id     uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  varchar(128) NOT NULL UNIQUE,
    expires_at  timestamp(6) with time zone NOT NULL,
    used_at     timestamp(6) with time zone,
    created_at  timestamp(6) with time zone NOT NULL
);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);

-- 4. New notification kinds for membership changes.
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (type IN (
    'TRIP_INVITATION', 'EXPENSE_ADDED', 'EXPENSE_SETTLED', 'DOCUMENT_UPLOADED', 'CHAT_MESSAGE',
    'TRIP_UPDATED', 'GENERAL', 'MEMBER_JOINED', 'MEMBER_REMOVED', 'ROLE_CHANGED'
));
