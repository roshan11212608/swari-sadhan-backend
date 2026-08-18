-- OTP records for shop-owner registration (email + mobile verification).
CREATE TABLE IF NOT EXISTS shop_reg_otp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    mobile_number VARCHAR(20) NOT NULL,
    email_otp_hash VARCHAR(255) NOT NULL,
    mobile_otp_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    verification_attempts INT NOT NULL DEFAULT 0,
    resend_count INT NOT NULL DEFAULT 0,
    last_sent_at DATETIME NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token_hash VARCHAR(255) NULL,
    token_expires_at DATETIME NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_shop_reg_otp_email_mobile (email, mobile_number)
);
