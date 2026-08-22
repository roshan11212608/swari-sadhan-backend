-- ============================================================
-- V46: Add unique constraint to prevent duplicate active/trial
-- subscriptions for the same shop owner.
--
-- MySQL does not support filtered/partial unique indexes, so we
-- use a generated column that is NULL for non-active/non-trial
-- statuses and the shop_owner_id otherwise. This makes the unique
-- index enforce "at most one ACTIVE-or-TRIAL subscription per
-- shop owner" while allowing multiple EXPIRED/CANCELLED rows.
-- ============================================================

ALTER TABLE subscriptions
    ADD COLUMN active_owner_key BIGINT GENERATED ALWAYS AS
        (CASE WHEN status IN ('ACTIVE', 'TRIAL') THEN shop_owner_id ELSE NULL END) STORED;

CREATE UNIQUE INDEX uk_subscriptions_active_owner
    ON subscriptions (active_owner_key);
