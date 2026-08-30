-- V55: Add vehicle rollover columns to subscriptions table
-- new_plan_vehicle_limit = base plan limit (monthly × cycle), excluding carry-forward
-- carried_forward_vehicle_limit = unused allowance from previous period
-- vehicle_limit_snapshot = total limit (new_plan_vehicle_limit + carried_forward_vehicle_limit)

ALTER TABLE subscriptions ADD COLUMN new_plan_vehicle_limit INT NULL;
ALTER TABLE subscriptions ADD COLUMN carried_forward_vehicle_limit INT DEFAULT 0;

-- Backfill existing rows: new_plan_vehicle_limit = vehicle_limit_snapshot, carry_forward = 0
UPDATE subscriptions SET new_plan_vehicle_limit = vehicle_limit_snapshot WHERE new_plan_vehicle_limit IS NULL;
UPDATE subscriptions SET carried_forward_vehicle_limit = 0 WHERE carried_forward_vehicle_limit IS NULL;
