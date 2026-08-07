-- Drop orphaned 'category' column that was created by Hibernate ddl-auto
-- before @JoinColumn(name = "category_id") was added to Expense entity.
-- The correct FK column is 'category_id' which already exists.
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'expenses' AND COLUMN_NAME = 'category');
SET @sql = IF(@col_exists > 0, 'ALTER TABLE expenses DROP COLUMN category', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
