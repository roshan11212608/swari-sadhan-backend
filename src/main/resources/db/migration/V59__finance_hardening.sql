-- V59: Finance module production hardening
--
-- 1. Fix business currency to NPR (Nepal). The subscription_settings row had
--    drifted to USD while all actual payments are recorded in NPR. Payment
--    gateways (eSewa, Fonepay) are Nepali and settle in NPR.
-- 2. Add coupon_code_snapshot to subscription_transactions so historical
--    transactions/invoices keep the coupon code even if the coupon is later
--    renamed or deleted (coupons are hard-deleted by SubscriptionCouponServiceImpl).
-- 3. Add missing indexes for finance reporting query patterns.
--
-- NOTE: Unique constraints already exist and are intentionally NOT re-created:
--   subscription_transactions.transaction_id  -> uk_sub_txn_id
--   subscription_transactions.invoice_number  -> uk_sub_invoice_number
--   payments.transaction_uuid                 -> uk_payments_transaction_uuid

-- ---------------------------------------------------------------------------
-- 1. Business currency: NPR
-- ---------------------------------------------------------------------------
UPDATE subscription_settings
SET currency = 'NPR'
WHERE id = 1;

ALTER TABLE subscription_settings
    ALTER COLUMN currency SET DEFAULT 'NPR';

-- ---------------------------------------------------------------------------
-- 2. Coupon code snapshot for historical accuracy
-- ---------------------------------------------------------------------------
ALTER TABLE subscription_transactions
    ADD COLUMN coupon_code_snapshot VARCHAR(50) NULL AFTER coupon_id;

-- Backfill from payments, which already stores couponCodeSnapshot.
-- Only touches rows that actually used a coupon; leaves all others NULL.
UPDATE subscription_transactions st
    JOIN payments p ON st.transaction_id = p.transaction_uuid
SET st.coupon_code_snapshot = p.coupon_code_snapshot
WHERE p.coupon_code_snapshot IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 3. Finance reporting indexes
-- ---------------------------------------------------------------------------

-- Gateway filter on the Super Admin transactions page
CREATE INDEX idx_subtxn_gateway ON subscription_transactions (gateway);

-- Invoice lookups (Invoices page + invoice search)
CREATE INDEX idx_payments_invoice_number ON payments (invoice_number);

-- Composite: the dominant dashboard/report pattern is
--   WHERE status = 'COMPLETED' AND transaction_date BETWEEN ? AND ?
-- and the plan-revenue aggregation groups by plan within that filter.
CREATE INDEX idx_subtxn_status_date ON subscription_transactions (status, transaction_date);
CREATE INDEX idx_subtxn_plan_status ON subscription_transactions (plan_id, status);
