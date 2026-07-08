-- Add bought_date column to vehicles table (if not exists)
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'swarisadhan' 
    AND TABLE_NAME = 'vehicles' 
    AND COLUMN_NAME = 'bought_date'
);

SET @sql = IF(@column_exists = 0, 
    'ALTER TABLE vehicles ADD COLUMN bought_date DATE', 
    'SELECT ''Column bought_date already exists'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
