-- Create homepage content tables for admin-managed landing page sections

CREATE TABLE IF NOT EXISTS homepage_brands (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    image_url TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_homepage_brands_active (is_active),
    INDEX idx_homepage_brands_order (display_order)
);

CREATE TABLE IF NOT EXISTS homepage_featured_vehicles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vehicle_id BIGINT,
    image_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_homepage_featured_vehicle (vehicle_id),
    INDEX idx_homepage_featured_active (is_active),
    INDEX idx_homepage_featured_order (display_order)
);

CREATE TABLE IF NOT EXISTS homepage_budgets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    max_price DECIMAL(19,2) NOT NULL,
    image_url TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_homepage_budgets_active (is_active),
    INDEX idx_homepage_budgets_order (display_order)
);

CREATE TABLE IF NOT EXISTS homepage_services (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    image_url TEXT NOT NULL,
    redirect_url VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_homepage_services_active (is_active),
    INDEX idx_homepage_services_order (display_order)
);
