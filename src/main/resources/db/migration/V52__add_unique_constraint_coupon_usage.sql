-- Add unique constraint on subscription_coupon_usages (coupon_id, transaction_id)
-- to prevent duplicate coupon usage recording from retried payment callbacks.
-- transaction_id references the payment ID, ensuring one coupon usage per payment.
ALTER TABLE subscription_coupon_usages
    ADD CONSTRAINT uk_coupon_usage_coupon_transaction
    UNIQUE (coupon_id, transaction_id);
