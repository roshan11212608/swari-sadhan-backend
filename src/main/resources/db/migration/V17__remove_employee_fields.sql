-- Remove unused fields from employees table
ALTER TABLE employees DROP COLUMN email;
ALTER TABLE employees DROP COLUMN aadhar_number;
ALTER TABLE employees DROP COLUMN education;
ALTER TABLE employees DROP COLUMN permanent_address;
