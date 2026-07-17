-- Make customer_id nullable in enquiries table to support guest enquiries
ALTER TABLE enquiries MODIFY COLUMN customer_id BIGINT NULL;
