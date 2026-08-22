-- ============================================================
-- V43: Subscription Management Module
-- Tables: plans, pricing, restrictions, features, coupons,
--         trial_config, subscriptions, transactions, coupon_usages,
--         activities, settings, invoice_sequence
-- ============================================================

-- 1. Subscription Plans
CREATE TABLE subscription_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description TEXT,
    short_description VARCHAR(255),
    category VARCHAR(20) NOT NULL DEFAULT 'BASIC',
    icon VARCHAR(50),
    theme_color VARCHAR(20) DEFAULT '#f97316',
    sort_order INT DEFAULT 0,
    is_popular BOOLEAN DEFAULT FALSE,
    is_recommended BOOLEAN DEFAULT FALSE,
    visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC',
    status VARCHAR(15) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    UNIQUE KEY uk_subscription_plans_slug (slug),
    INDEX idx_subscription_plans_status (status),
    INDEX idx_subscription_plans_visibility (visibility),
    INDEX idx_subscription_plans_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Subscription Plan Pricing
CREATE TABLE subscription_plan_pricing (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    monthly DECIMAL(12,2),
    quarterly DECIMAL(12,2),
    half_yearly DECIMAL(12,2),
    yearly DECIMAL(12,2),
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    gst_included BOOLEAN DEFAULT TRUE,
    discount_percentage INT DEFAULT 0,
    strike_price DECIMAL(12,2),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_pricing_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id) ON DELETE CASCADE,
    INDEX idx_pricing_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Subscription Plan Restrictions
CREATE TABLE subscription_plan_restrictions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    max_vehicles INT,
    max_employees INT,
    max_storage VARCHAR(20),
    max_branches INT,
    api_calls INT,
    support_level VARCHAR(50),
    daily_upload_limit INT,
    backup_frequency VARCHAR(30),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_restrictions_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id) ON DELETE CASCADE,
    INDEX idx_restrictions_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Subscription Plan Features
CREATE TABLE subscription_plan_features (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(50),
    description VARCHAR(255),
    included BOOLEAN DEFAULT FALSE,
    `limit` INT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_features_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id) ON DELETE CASCADE,
    INDEX idx_features_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Subscription Coupons (before transactions)
CREATE TABLE subscription_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    discount_type VARCHAR(15) NOT NULL DEFAULT 'PERCENTAGE',
    percentage INT,
    flat_discount DECIMAL(12,2),
    maximum_discount DECIMAL(12,2),
    minimum_purchase DECIMAL(12,2),
    usage_limit INT NOT NULL DEFAULT 100,
    expiry_date DATE,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    UNIQUE KEY uk_coupon_code (code),
    INDEX idx_coupon_expiry_date (expiry_date),
    INDEX idx_coupon_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Subscription Trial Configuration (single row, before subscriptions)
CREATE TABLE subscription_trial_config (
    id BIGINT PRIMARY KEY DEFAULT 1,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    duration INT NOT NULL DEFAULT 14,
    eligibility_rules VARCHAR(500),
    maximum_uses INT NOT NULL DEFAULT 100,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT chk_trial_single_row CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Subscriptions (references plans and trial_config)
CREATE TABLE subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_owner_id BIGINT NOT NULL,
    shop_id BIGINT,
    plan_id BIGINT NOT NULL,
    trial_id BIGINT,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    auto_renewal BOOLEAN DEFAULT FALSE,
    status VARCHAR(15) NOT NULL DEFAULT 'ACTIVE',
    renewal_date DATETIME,
    cancelled_date DATETIME,
    suspended_date DATETIME,
    reason VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id),
    CONSTRAINT fk_subscriptions_trial FOREIGN KEY (trial_id) REFERENCES subscription_trial_config(id),
    INDEX idx_subscriptions_shop_owner_id (shop_owner_id),
    INDEX idx_subscriptions_plan_id (plan_id),
    INDEX idx_subscriptions_status (status),
    INDEX idx_subscriptions_end_date (end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Subscription Transactions (references subscriptions, plans, coupons)
CREATE TABLE subscription_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL,
    subscription_id BIGINT,
    shop_owner_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    tax DECIMAL(12,2) DEFAULT 0,
    coupon_id BIGINT,
    discount DECIMAL(12,2) DEFAULT 0,
    final_amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(30),
    gateway VARCHAR(30),
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    invoice_number VARCHAR(50) NOT NULL,
    transaction_date DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    UNIQUE KEY uk_sub_txn_id (transaction_id),
    UNIQUE KEY uk_sub_invoice_number (invoice_number),
    CONSTRAINT fk_subtxn_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id),
    CONSTRAINT fk_subtxn_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id),
    CONSTRAINT fk_subtxn_coupon FOREIGN KEY (coupon_id) REFERENCES subscription_coupons(id),
    INDEX idx_subtxn_transaction_date (transaction_date),
    INDEX idx_subtxn_status (status),
    INDEX idx_subtxn_shop_owner_id (shop_owner_id),
    INDEX idx_subtxn_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. Subscription Coupon Usages (references coupons and transactions)
CREATE TABLE subscription_coupon_usages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,
    shop_owner_id BIGINT NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    used_at DATETIME NOT NULL,
    CONSTRAINT fk_coupon_usage_coupon FOREIGN KEY (coupon_id) REFERENCES subscription_coupons(id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_usage_txn FOREIGN KEY (transaction_id) REFERENCES subscription_transactions(id),
    INDEX idx_coupon_usage_coupon_id (coupon_id),
    INDEX idx_coupon_usage_txn_id (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. Subscription Activities (audit log)
CREATE TABLE subscription_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    admin_user_id BIGINT,
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'COMPLETED',
    created_at DATETIME NOT NULL,
    INDEX idx_activities_entity (entity_type, entity_id),
    INDEX idx_activities_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. Subscription Settings (single row)
CREATE TABLE subscription_settings (
    id BIGINT PRIMARY KEY DEFAULT 1,
    default_trial_days INT NOT NULL DEFAULT 14,
    tax_percentage INT NOT NULL DEFAULT 18,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    invoice_prefix VARCHAR(20) NOT NULL DEFAULT 'INV',
    payment_reminder_days INT NOT NULL DEFAULT 7,
    renewal_reminder INT NOT NULL DEFAULT 3,
    grace_period INT NOT NULL DEFAULT 5,
    cancellation_policy TEXT,
    refund_policy TEXT,
    enable_auto_renewal BOOLEAN DEFAULT TRUE,
    enable_free_trial BOOLEAN DEFAULT TRUE,
    enable_coupons BOOLEAN DEFAULT TRUE,
    enable_lifetime_plans BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT chk_settings_single_row CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. Invoice sequence table for unique invoice numbers
CREATE TABLE subscription_invoice_sequence (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    year INT NOT NULL,
    next_val INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_invoice_seq_year (year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default trial configuration
INSERT INTO subscription_trial_config (id, name, description, duration, eligibility_rules, maximum_uses, active, created_at)
VALUES (1, 'Newcomer Free Trial', 'Free trial for new shop owners to explore the platform', 14, 'New shop owners only (first-time registration)', 100, TRUE, NOW());

-- Insert default subscription settings
INSERT INTO subscription_settings (id, default_trial_days, tax_percentage, currency, invoice_prefix, payment_reminder_days, renewal_reminder, grace_period, cancellation_policy, refund_policy, enable_auto_renewal, enable_free_trial, enable_coupons, enable_lifetime_plans, created_at)
VALUES (1, 14, 18, 'INR', 'INV', 7, 3, 5, 'Users can cancel anytime. Refunds processed within 7 days.', 'Full refund within 7 days, prorated refund after that.', TRUE, TRUE, TRUE, TRUE, NOW());
