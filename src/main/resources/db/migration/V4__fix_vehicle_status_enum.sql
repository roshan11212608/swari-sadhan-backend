-- Fix vehicles table status enum to include all values from Java enum
ALTER TABLE vehicles MODIFY COLUMN status ENUM('ACTIVE', 'INACTIVE', 'SOLD', 'PENDING_APPROVAL', 'REJECTED', 'SUSPENDED', 'FLAGGED', 'PENDING_SALE') DEFAULT 'PENDING_APPROVAL';
