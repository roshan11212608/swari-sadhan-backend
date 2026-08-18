-- Add a human-friendly customer code (e.g. 12082026-001) for public users.
-- This is a display identifier, separate from the numeric primary key.
ALTER TABLE users ADD COLUMN customer_code VARCHAR(20) NULL;

-- Create an index for fast lookup during code generation
CREATE INDEX idx_users_customer_code ON users(customer_code);
