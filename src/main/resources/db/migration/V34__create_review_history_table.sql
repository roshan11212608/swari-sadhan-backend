CREATE TABLE IF NOT EXISTS public_vehicle_listing_review_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    actor VARCHAR(20) NOT NULL,
    action VARCHAR(30) NOT NULL,
    reason TEXT NULL,
    notes TEXT NULL,
    performed_at DATETIME NOT NULL,
    INDEX idx_rvh_listing_id (listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
