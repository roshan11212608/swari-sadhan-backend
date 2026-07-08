-- Add missing fields to sell_vehicle_applications table
-- Use ALTER TABLE with IF NOT EXISTS logic for MySQL
SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns
                      WHERE table_schema = 'swarisadhan'
                      AND table_name = 'sell_vehicle_applications'
                      AND column_name = 'customer_parent_name');

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE sell_vehicle_applications ADD COLUMN customer_parent_name VARCHAR(255) AFTER customer_name',
    'SELECT ''Column customer_parent_name already exists''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns
                      WHERE table_schema = 'swarisadhan'
                      AND table_name = 'sell_vehicle_applications'
                      AND column_name = 'customer_photo');

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE sell_vehicle_applications ADD COLUMN customer_photo TEXT AFTER customer_citizenship_number',
    'SELECT ''Column customer_photo already exists''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns
                      WHERE table_schema = 'swarisadhan'
                      AND table_name = 'sell_vehicle_applications'
                      AND column_name = 'citizenship_front_photo');

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE sell_vehicle_applications ADD COLUMN citizenship_front_photo TEXT AFTER customer_photo',
    'SELECT ''Column citizenship_front_photo already exists''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns
                      WHERE table_schema = 'swarisadhan'
                      AND table_name = 'sell_vehicle_applications'
                      AND column_name = 'citizenship_back_photo');

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE sell_vehicle_applications ADD COLUMN citizenship_back_photo TEXT AFTER citizenship_front_photo',
    'SELECT ''Column citizenship_back_photo already exists''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns
                      WHERE table_schema = 'swarisadhan'
                      AND table_name = 'sell_vehicle_applications'
                      AND column_name = 'offered_price_in_words');

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE sell_vehicle_applications ADD COLUMN offered_price_in_words TEXT AFTER offered_price',
    'SELECT ''Column offered_price_in_words already exists''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns
                      WHERE table_schema = 'swarisadhan'
                      AND table_name = 'sell_vehicle_applications'
                      AND column_name = 'sales_man_name');

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE sell_vehicle_applications ADD COLUMN sales_man_name VARCHAR(255) AFTER financing_amount',
    'SELECT ''Column sales_man_name already exists''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
