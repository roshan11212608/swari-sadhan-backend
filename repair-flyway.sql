-- Repair Flyway migration by removing failed V7 migration
DELETE FROM flyway_schema_history WHERE version = 7;

-- Remove any partially added columns from V7 migration
ALTER TABLE sell_vehicle_applications DROP COLUMN IF EXISTS customer_parent_name;
ALTER TABLE sell_vehicle_applications DROP COLUMN IF EXISTS customer_photo;
ALTER TABLE sell_vehicle_applications DROP COLUMN IF EXISTS citizenship_front_photo;
ALTER TABLE sell_vehicle_applications DROP COLUMN IF EXISTS citizenship_back_photo;
ALTER TABLE sell_vehicle_applications DROP COLUMN IF EXISTS offered_price_in_words;
ALTER TABLE sell_vehicle_applications DROP COLUMN IF EXISTS sales_man_name;
