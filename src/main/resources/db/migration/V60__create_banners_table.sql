-- Create banners table for managing banner images across the platform
CREATE TABLE IF NOT EXISTS banners (
    id BIGINT NOT NULL AUTO_INCREMENT,
    image_url TEXT NOT NULL,
    title VARCHAR(255),
    position VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_banners_position (position),
    INDEX idx_banners_is_active (is_active)
);
