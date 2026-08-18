-- OTP records for the forgot-password flow (email-only verification).
CREATE TABLE IF NOT EXISTS password_reset_otp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    verification_attempts INT NOT NULL DEFAULT 0,
    resend_count INT NOT NULL DEFAULT 0,
    last_sent_at DATETIME NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_password_reset_otp_email (email)
);
