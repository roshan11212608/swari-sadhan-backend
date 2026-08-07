-- Remove PAN number column from employees table
-- MySQL 8.0 does not support DROP INDEX/COLUMN IF EXISTS, so use conditional logic

DELIMITER //
DROP PROCEDURE IF EXISTS drop_pan_number_column//
CREATE PROCEDURE drop_pan_number_column()
BEGIN
    -- Drop unique index on pan_number if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'employees'
          AND COLUMN_NAME = 'pan_number'
          AND NON_UNIQUE = 0
    ) THEN
        SET @index_name = (
            SELECT DISTINCT INDEX_NAME FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'employees'
              AND COLUMN_NAME = 'pan_number'
              AND NON_UNIQUE = 0
            LIMIT 1
        );
        SET @ddl = CONCAT('ALTER TABLE employees DROP INDEX `', @index_name, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    -- Drop pan_number column if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'employees'
          AND COLUMN_NAME = 'pan_number'
    ) THEN
        ALTER TABLE employees DROP COLUMN pan_number;
    END IF;
END//
DELIMITER ;

CALL drop_pan_number_column();
DROP PROCEDURE IF EXISTS drop_pan_number_column;
