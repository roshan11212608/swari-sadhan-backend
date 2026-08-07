-- Remove CASCADE DELETE from employee-related foreign keys to support soft delete
-- CASCADE at DB level bypasses soft delete logic. Application layer handles cascading soft deletes.

-- Drop and recreate employees.shop_id FK without CASCADE
ALTER TABLE employees DROP FOREIGN KEY employees_ibfk_1;
ALTER TABLE employees 
ADD CONSTRAINT fk_employees_shop 
FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE RESTRICT;

-- Drop and recreate attendance.employee_id FK without CASCADE
ALTER TABLE attendance DROP FOREIGN KEY attendance_ibfk_1;
ALTER TABLE attendance 
ADD CONSTRAINT fk_attendance_employee 
FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT;

-- Drop and recreate salary_records.employee_id FK without CASCADE
ALTER TABLE salary_records DROP FOREIGN KEY salary_records_ibfk_1;
ALTER TABLE salary_records 
ADD CONSTRAINT fk_salary_records_employee 
FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT;

-- Drop and recreate leave_requests.employee_id FK without CASCADE
ALTER TABLE leave_requests DROP FOREIGN KEY leave_requests_ibfk_1;
ALTER TABLE leave_requests 
ADD CONSTRAINT fk_leave_requests_employee 
FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT;

-- Drop and recreate advance_payments.employee_id FK without CASCADE
ALTER TABLE advance_payments DROP FOREIGN KEY advance_payments_ibfk_1;
ALTER TABLE advance_payments 
ADD CONSTRAINT fk_advance_payments_employee 
FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT;
