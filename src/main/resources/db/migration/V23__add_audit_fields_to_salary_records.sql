-- Add created_at and updated_at columns to salary_records for consistent audit trail
-- These fields were missing from the original schema but are present in other employee-related tables

ALTER TABLE salary_records 
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE salary_records 
ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
