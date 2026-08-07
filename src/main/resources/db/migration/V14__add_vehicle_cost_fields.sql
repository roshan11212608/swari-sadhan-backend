-- Add vehicle cost tracking fields for accurate profit calculation
-- These fields are required for proper inventory accounting in the Analytics module

ALTER TABLE vehicles 
ADD COLUMN purchase_price DECIMAL(12,2) DEFAULT 0.00 COMMENT 'Purchase cost of the vehicle',
ADD COLUMN repair_cost DECIMAL(12,2) DEFAULT 0.00 COMMENT 'Cost of repairs/refurbishment',
ADD COLUMN additional_expenses DECIMAL(12,2) DEFAULT 0.00 COMMENT 'Additional vehicle expenses (transport, documentation, etc.)';

-- Add indexes for analytics queries
ALTER TABLE vehicles 
ADD INDEX idx_vehicle_shop_status (shop_id, status),
ADD INDEX idx_vehicle_sold_at (sold_at),
ADD INDEX idx_vehicle_bought_date (bought_date);
