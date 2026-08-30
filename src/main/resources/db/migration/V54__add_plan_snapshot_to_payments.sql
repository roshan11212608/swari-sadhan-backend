-- V54: Add plan snapshot columns to payments table
-- These capture the plan name and subscription period at the moment the
-- payment was completed. The Subscription entity is reused on
-- renewal/upgrade (its snapshots are overwritten), so these Payment-level
-- snapshots are the only historically immutable record of what plan the
-- user had at each billing period.

ALTER TABLE payments ADD COLUMN plan_name_snapshot VARCHAR(100) NULL;
ALTER TABLE payments ADD COLUMN subscription_start_date_snapshot DATETIME NULL;
ALTER TABLE payments ADD COLUMN subscription_end_date_snapshot DATETIME NULL;
ALTER TABLE payments ADD COLUMN vehicle_limit_snapshot INT NULL;
