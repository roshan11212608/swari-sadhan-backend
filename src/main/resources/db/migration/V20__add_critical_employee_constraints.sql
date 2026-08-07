-- Add critical database constraints for Employee Management module (Phase 1)
-- Defensive: clean up bad data and conditionally add constraints

-- Step 1: Clean up any data that would violate the constraints
UPDATE salary_records SET amount_paid = 0 WHERE amount_paid < 0 OR amount_paid IS NULL;
UPDATE salary_records SET total_payable = 0 WHERE total_payable < 0 OR total_payable IS NULL;
UPDATE salary_records SET net_salary = 0 WHERE net_salary < 0 OR net_salary IS NULL;
UPDATE advance_payments SET advance_amount = 1 WHERE advance_amount <= 0 OR advance_amount IS NULL;
UPDATE advance_payments SET recovered_amount = 0 WHERE recovered_amount < 0 OR recovered_amount IS NULL;
UPDATE advance_payments SET remaining_balance = 0 WHERE remaining_balance < 0 OR remaining_balance IS NULL;
UPDATE employees SET basic_salary = 1 WHERE basic_salary <= 0 OR basic_salary IS NULL;

-- Step 2: Conditionally add constraints using stored procedure
DELIMITER //
DROP PROCEDURE IF EXISTS add_constraint_if_not_exists//
CREATE PROCEDURE add_constraint_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_constraint VARCHAR(64),
    IN p_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table
          AND CONSTRAINT_NAME = p_constraint
    ) THEN
        SET @ddl = p_sql;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_constraint_if_not_exists('salary_records', 'chk_salary_amount_paid_non_negative',
    'ALTER TABLE salary_records ADD CONSTRAINT chk_salary_amount_paid_non_negative CHECK (amount_paid >= 0)');

CALL add_constraint_if_not_exists('salary_records', 'chk_salary_total_payable_non_negative',
    'ALTER TABLE salary_records ADD CONSTRAINT chk_salary_total_payable_non_negative CHECK (total_payable >= 0)');

CALL add_constraint_if_not_exists('salary_records', 'chk_salary_net_salary_non_negative',
    'ALTER TABLE salary_records ADD CONSTRAINT chk_salary_net_salary_non_negative CHECK (net_salary >= 0)');

CALL add_constraint_if_not_exists('advance_payments', 'chk_advance_amount_positive',
    'ALTER TABLE advance_payments ADD CONSTRAINT chk_advance_amount_positive CHECK (advance_amount > 0)');

CALL add_constraint_if_not_exists('advance_payments', 'chk_advance_recovered_non_negative',
    'ALTER TABLE advance_payments ADD CONSTRAINT chk_advance_recovered_non_negative CHECK (recovered_amount >= 0)');

CALL add_constraint_if_not_exists('advance_payments', 'chk_advance_remaining_non_negative',
    'ALTER TABLE advance_payments ADD CONSTRAINT chk_advance_remaining_non_negative CHECK (remaining_balance >= 0)');

CALL add_constraint_if_not_exists('employees', 'chk_employee_basic_salary_positive',
    'ALTER TABLE employees ADD CONSTRAINT chk_employee_basic_salary_positive CHECK (basic_salary > 0)');

DROP PROCEDURE IF EXISTS add_constraint_if_not_exists;
