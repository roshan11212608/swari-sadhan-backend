-- Add coupon snapshot fields to payments table for historical preservation.
-- These fields freeze the coupon details at payment creation time so that
-- later admin edits/deletes of the coupon do not alter historical records.
ALTER TABLE payments ADD COLUMN coupon_code_snapshot VARCHAR(50) DEFAULT NULL;
ALTER TABLE payments ADD COLUMN coupon_discount_type_snapshot VARCHAR(20) DEFAULT NULL;
ALTER TABLE payments ADD COLUMN coupon_discount_value_snapshot VARCHAR(50) DEFAULT NULL;

-- Backfill existing payments that have a coupon_id but no snapshot
UPDATE payments p
INNER JOIN subscription_coupons c ON p.coupon_id = c.id
SET
    p.coupon_code_snapshot = c.code,
    p.coupon_discount_type_snapshot = c.discount_type,
    p.coupon_discount_value_snapshot = CASE
        WHEN c.discount_type = 'PERCENTAGE' THEN CONCAT(c.percentage, '%')
        WHEN c.discount_type = 'FLAT' THEN c.flat_discount
        ELSE NULL
    END
WHERE p.coupon_id IS NOT NULL
  AND p.coupon_code_snapshot IS NULL;
