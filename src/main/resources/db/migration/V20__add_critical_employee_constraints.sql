-- Add critical database constraints for Employee Management module (Phase 1)
-- TiDB-compatible version without stored procedures

-- Step 1: Clean up any data that would violate the constraints
UPDATE salary_records SET amount_paid = 0 WHERE amount_paid < 0 OR amount_paid IS NULL;
UPDATE salary_records SET total_payable = 0 WHERE total_payable < 0 OR total_payable IS NULL;
UPDATE salary_records SET net_salary = 0 WHERE net_salary < 0 OR net_salary IS NULL;
UPDATE advance_payments SET advance_amount = 1 WHERE advance_amount <= 0 OR advance_amount IS NULL;
UPDATE advance_payments SET recovered_amount = 0 WHERE recovered_amount < 0 OR recovered_amount IS NULL;
UPDATE advance_payments SET remaining_balance = 0 WHERE remaining_balance < 0 OR remaining_balance IS NULL;
UPDATE employees SET basic_salary = 1 WHERE basic_salary <= 0 OR basic_salary IS NULL;

-- Step 2: Add CHECK constraints
ALTER TABLE salary_records ADD CONSTRAINT chk_salary_amount_paid_non_negative CHECK (amount_paid >= 0);
ALTER TABLE salary_records ADD CONSTRAINT chk_salary_total_payable_non_negative CHECK (total_payable >= 0);
ALTER TABLE salary_records ADD CONSTRAINT chk_salary_net_salary_non_negative CHECK (net_salary >= 0);
ALTER TABLE advance_payments ADD CONSTRAINT chk_advance_amount_positive CHECK (advance_amount > 0);
ALTER TABLE advance_payments ADD CONSTRAINT chk_advance_recovered_non_negative CHECK (recovered_amount >= 0);
ALTER TABLE advance_payments ADD CONSTRAINT chk_advance_remaining_non_negative CHECK (remaining_balance >= 0);
ALTER TABLE employees ADD CONSTRAINT chk_employee_basic_salary_positive CHECK (basic_salary > 0);
