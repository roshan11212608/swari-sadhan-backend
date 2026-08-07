-- Create Public Vehicle Listing tables for marketplace seller submissions

CREATE TABLE IF NOT EXISTS public_vehicle_listings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_number VARCHAR(30) NOT NULL UNIQUE,
    seller_user_id BIGINT,

    -- Seller Information
    seller_name VARCHAR(255) NOT NULL,
    seller_phone VARCHAR(20) NOT NULL,
    seller_email VARCHAR(255),
    seller_city VARCHAR(100) NOT NULL,
    seller_area VARCHAR(100),
    preferred_contact_method VARCHAR(50),

    -- Owner Information
    owner_name VARCHAR(255) NOT NULL,
    number_of_owners INT,
    ownership_status VARCHAR(50) NOT NULL,
    owner_address TEXT,

    -- Vehicle Information
    vehicle_number VARCHAR(50) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    variant VARCHAR(100),
    manufacturing_year INT NOT NULL,
    registration_year INT NOT NULL,
    kilometers_driven INT NOT NULL,
    fuel_type VARCHAR(50) NOT NULL,
    transmission_type VARCHAR(50),
    engine_cc VARCHAR(20),
    color VARCHAR(50) NOT NULL,

    -- Condition
    vehicle_condition VARCHAR(50),
    accident_history VARCHAR(10),
    accident_details TEXT,
    major_repairs VARCHAR(10),
    repair_details TEXT,
    known_issues TEXT,

    -- Description
    vehicle_description TEXT,
    reason_for_selling TEXT,

    -- Pricing
    price DECIMAL(19, 2) NOT NULL,
    price_in_words TEXT,
    negotiable BOOLEAN DEFAULT FALSE,
    price_includes VARCHAR(255),

    -- Admin / Status
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    admin_notes TEXT,
    rejection_reason TEXT,
    declaration_accepted BOOLEAN DEFAULT FALSE,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP NULL,
    reviewed_at TIMESTAMP NULL,
    approved_at TIMESTAMP NULL,
    published_at TIMESTAMP NULL,
    sold_at TIMESTAMP NULL,

    INDEX idx_pvl_status (status),
    INDEX idx_pvl_listing_number (listing_number),
    INDEX idx_pvl_seller_user (seller_user_id),
    INDEX idx_pvl_vehicle_number (vehicle_number),
    INDEX idx_pvl_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS public_vehicle_listing_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    file_url TEXT NOT NULL,
    original_filename VARCHAR(255),
    file_type VARCHAR(30) NOT NULL,
    document_type VARCHAR(50),
    is_public BOOLEAN DEFAULT FALSE,
    is_cover BOOLEAN DEFAULT FALSE,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (listing_id) REFERENCES public_vehicle_listings(id) ON DELETE CASCADE,
    INDEX idx_pvlf_listing (listing_id),
    INDEX idx_pvlf_file_type (file_type),
    INDEX idx_pvlf_is_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
