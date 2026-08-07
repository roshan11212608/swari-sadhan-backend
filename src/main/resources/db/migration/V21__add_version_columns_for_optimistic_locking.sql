-- Add version columns for optimistic locking to prevent concurrent modification issues
-- This adds @Version field support for JPA optimistic locking

ALTER TABLE salary_records ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE advance_payments ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE leave_requests ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE attendance ADD COLUMN version BIGINT DEFAULT 0;

-- Add indexes for version columns (optional but can help with query performance)
CREATE INDEX idx_salary_records_version ON salary_records(version);
CREATE INDEX idx_advance_payments_version ON advance_payments(version);
CREATE INDEX idx_leave_requests_version ON leave_requests(version);
CREATE INDEX idx_attendance_version ON attendance(version);
