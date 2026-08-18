-- OTP verification records for the mobile-based public signup flow.
-- Only a hash of the OTP is stored; the raw OTP is never persisted.
CREATE TABLE IF NOT EXISTS signup_otp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mobile_number VARCHAR(20) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    verification_attempts INT NOT NULL DEFAULT 0,
    resend_count INT NOT NULL DEFAULT 0,
    last_sent_at DATETIME NULL,
    verified TINYINT(1) NOT NULL DEFAULT 0,
    verification_token_hash VARCHAR(255) NULL,
    token_expires_at DATETIME NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_signup_otp_mobile (mobile_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
