-- Remove PAN number column from employees table
-- TiDB-compatible version using IF EXISTS instead of stored procedures

DROP INDEX IF EXISTS pan_number ON employees;

ALTER TABLE employees DROP COLUMN IF EXISTS pan_number;
