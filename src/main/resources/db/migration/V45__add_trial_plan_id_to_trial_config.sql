-- ============================================================
-- V45: Add trial_plan_id to subscription_trial_config
-- Allows the admin to configure which published plan is used
-- for trial subscriptions.
-- ============================================================

ALTER TABLE subscription_trial_config
    ADD COLUMN trial_plan_id BIGINT NULL AFTER maximum_uses;

-- Foreign key to subscription_plans
ALTER TABLE subscription_trial_config
    ADD CONSTRAINT fk_trial_config_plan
        FOREIGN KEY (trial_plan_id) REFERENCES subscription_plans(id);

-- Default the trial plan to the Starter plan (id=5) if it exists
UPDATE subscription_trial_config
SET trial_plan_id = (SELECT id FROM subscription_plans WHERE name = 'Starter' LIMIT 1)
WHERE trial_plan_id IS NULL
  AND EXISTS (SELECT 1 FROM subscription_plans WHERE name = 'Starter' LIMIT 1);
