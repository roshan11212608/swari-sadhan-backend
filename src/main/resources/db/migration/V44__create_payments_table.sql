-- V44: Create payments table for eSewa payment gateway integration

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_uuid VARCHAR(100) NOT NULL,
    gateway VARCHAR(50) NOT NULL,
    gateway_transaction_id VARCHAR(100),
    gateway_ref_id VARCHAR(100),
    shop_owner_id BIGINT NOT NULL,
    subscription_plan_id BIGINT NOT NULL,
    subscription_id BIGINT,
    billing_cycle VARCHAR(30),
    amount DECIMAL(12, 2) NOT NULL,
    tax_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'NPR',
    status VARCHAR(30) NOT NULL,
    payment_method VARCHAR(50),
    invoice_number VARCHAR(100),
    failure_reason VARCHAR(500),
    product_code VARCHAR(50),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    paid_at DATETIME,
    CONSTRAINT uk_payments_transaction_uuid UNIQUE (transaction_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_payments_shop_owner_id ON payments(shop_owner_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_subscription_plan_id ON payments(subscription_plan_id);
CREATE INDEX idx_payments_subscription_id ON payments(subscription_id);
