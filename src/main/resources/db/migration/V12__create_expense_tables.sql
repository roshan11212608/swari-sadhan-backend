-- Expense module tables for shop owner expense management
-- This migration creates tables for expenses, expense categories, and expense attachments

-- Expense categories table
CREATE TABLE IF NOT EXISTS expense_categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    color       VARCHAR(7)   NOT NULL DEFAULT '#f97316',
    icon        VARCHAR(10)  NOT NULL DEFAULT '📁',
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ec_name (name),
    KEY idx_ec_active (is_active)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Insert predefined expense categories
INSERT INTO expense_categories (name, color, icon) VALUES
('Shop Rent', '#8b5cf6', '🏢'),
('Electricity Bill', '#eab308', '⚡'),
('Water Bill', '#06b6d4', '💧'),
('Employee Salary', '#10b981', '💰'),
('Internet Bill', '#0ea5e9', '🌐'),
('Marketing', '#f97316', '📢'),
('Office Supplies', '#3b82f6', '📁'),
('Tea & Snacks', '#f59e0b', '☕'),
('Cleaning', '#14b8a6', '🧼'),
('Accountant Fees', '#6366f1', '📊'),
('Miscellaneous', '#6b7280', '📦');

-- Expenses table
CREATE TABLE IF NOT EXISTS expenses (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    expense_number      VARCHAR(50)  NOT NULL UNIQUE,
    shop_id             BIGINT       NOT NULL,
    category_id         BIGINT       NOT NULL,
    title               VARCHAR(255) NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    expense_date        DATE         NOT NULL,
    description         TEXT,
    notes               TEXT,
    vendor_paid_to      VARCHAR(255),
    payment_method      VARCHAR(20)  NOT NULL,
    payment_status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reference_number    VARCHAR(100),
    due_date            DATE,
    attachment_path     VARCHAR(500),
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by          VARCHAR(150) NOT NULL,
    updated_by          VARCHAR(150),
    PRIMARY KEY (id),
    UNIQUE KEY idx_expense_number (expense_number),
    KEY idx_expense_shop (shop_id),
    KEY idx_expense_category (category_id),
    KEY idx_expense_status (payment_status),
    KEY idx_expense_date (expense_date),
    KEY idx_expense_active (is_active),
    CONSTRAINT fk_expense_shop FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_category FOREIGN KEY (category_id) REFERENCES expense_categories(id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Expense attachments table
CREATE TABLE IF NOT EXISTS expense_attachments (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    expense_id      BIGINT       NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    file_path       VARCHAR(500) NOT NULL,
    file_size       BIGINT       NOT NULL,
    file_type       VARCHAR(50)  NOT NULL,
    uploaded_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_attachment_expense (expense_id),
    CONSTRAINT fk_attachment_expense FOREIGN KEY (expense_id) REFERENCES expenses(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
