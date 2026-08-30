-- Add dedicated selling_price column to vehicles
-- Existing vehicles inherit their current selling amount from the price column

ALTER TABLE vehicles 
ADD COLUMN selling_price DECIMAL(12,2) COMMENT 'Selling price of the vehicle';

UPDATE vehicles SET selling_price = price;
