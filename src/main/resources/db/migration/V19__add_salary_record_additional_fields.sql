-- Add additional fields to salary_records table
ALTER TABLE salary_records 
ADD COLUMN employee_id_number VARCHAR(50),
ADD COLUMN shop_name VARCHAR(255),
ADD COLUMN shop_location VARCHAR(255),
ADD COLUMN available_days INT,
ADD COLUMN paid_days INT,
ADD COLUMN loss_of_pay_days INT;
