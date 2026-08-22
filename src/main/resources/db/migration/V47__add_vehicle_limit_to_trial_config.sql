-- Add vehicle_limit column to subscription_trial_config
-- Allows admin to set a custom vehicle limit for trial subscriptions
-- If NULL, falls back to the trial plan's restriction
ALTER TABLE subscription_trial_config ADD COLUMN vehicle_limit INT NULL;
