-- Make department column nullable in employees table
ALTER TABLE employees MODIFY COLUMN department VARCHAR(100) NULL;
