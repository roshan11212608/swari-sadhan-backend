-- Add deleted_at column to employee-related tables for soft delete support
ALTER TABLE attendance ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE salary_records ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE leave_requests ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE advance_payments ADD COLUMN deleted_at TIMESTAMP NULL;

-- Add indexes for deleted_at columns
CREATE INDEX idx_attendance_deleted_at ON attendance(deleted_at);
CREATE INDEX idx_salary_records_deleted_at ON salary_records(deleted_at);
CREATE INDEX idx_leave_requests_deleted_at ON leave_requests(deleted_at);
CREATE INDEX idx_advance_payments_deleted_at ON advance_payments(deleted_at);
