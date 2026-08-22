-- V48: Add plan snapshot columns to subscriptions table.
-- These columns freeze plan details at the time of subscription so that
-- later admin changes (edit, delete, unpublish, modify) do not affect
-- existing subscriptions.
ALTER TABLE subscriptions ADD COLUMN plan_name_snapshot VARCHAR(255) NULL;
ALTER TABLE subscriptions ADD COLUMN plan_description_snapshot TEXT NULL;
ALTER TABLE subscriptions ADD COLUMN plan_icon_snapshot VARCHAR(255) NULL;
ALTER TABLE subscriptions ADD COLUMN plan_theme_color_snapshot VARCHAR(50) NULL;
ALTER TABLE subscriptions ADD COLUMN vehicle_limit_snapshot INT NULL;
ALTER TABLE subscriptions ADD COLUMN price_paid DECIMAL(10, 2) NULL;
ALTER TABLE subscriptions ADD COLUMN billing_cycle_snapshot VARCHAR(50) NULL;

-- Backfill existing subscriptions from their linked plans
UPDATE subscriptions s
INNER JOIN subscription_plans p ON s.plan_id = p.id
SET s.plan_name_snapshot = p.name,
    s.plan_description_snapshot = IFNULL(p.short_description, p.description),
    s.plan_icon_snapshot = p.icon,
    s.plan_theme_color_snapshot = p.theme_color
WHERE s.plan_name_snapshot IS NULL;

-- Backfill vehicle_limit_snapshot from plan restrictions
UPDATE subscriptions s
INNER JOIN subscription_plan_restrictions r ON s.plan_id = r.plan_id
SET s.vehicle_limit_snapshot = r.max_vehicles
WHERE s.vehicle_limit_snapshot IS NULL AND r.max_vehicles IS NOT NULL;

-- Backfill price_paid and billing_cycle from the successful payment
UPDATE subscriptions s
INNER JOIN (
    SELECT shop_owner_id, amount, billing_cycle
    FROM payments
    WHERE status = 'SUCCESS'
    ORDER BY paid_at DESC
) p ON p.shop_owner_id = s.shop_owner_id
SET s.price_paid = p.amount,
    s.billing_cycle_snapshot = p.billing_cycle
WHERE s.price_paid IS NULL;
