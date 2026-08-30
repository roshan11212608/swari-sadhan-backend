-- V53: Add current_period_start to subscriptions.
--
-- This field tracks the start of the CURRENT billing period for vehicle
-- allowance counting. On renewal/extension, currentPeriodStart moves to
-- the old endDate so the new period gets a fresh vehicle allowance, while
-- startDate preserves the original subscription creation date for history.
--
-- For existing subscriptions, currentPeriodStart defaults to startDate.
ALTER TABLE subscriptions
    ADD COLUMN current_period_start DATETIME NULL AFTER start_date;

UPDATE subscriptions SET current_period_start = start_date WHERE current_period_start IS NULL;

ALTER TABLE subscriptions
    MODIFY COLUMN current_period_start DATETIME NOT NULL;
