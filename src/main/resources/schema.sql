-- Swari Sewa Database Schema
-- SaaS-based Vehicle Recondition Shop Management System

-- Create Database
CREATE DATABASE IF NOT EXISTS swari_sewa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE swari_sewa;

-- Users Table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone_number VARCHAR(20),
    role ENUM('SUPER_ADMIN', 'SHOP_OWNER', 'CUSTOMER') NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    is_email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_active (is_active)
);

-- Categories Table
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_active (is_active)
);

-- Shops Table
CREATE TABLE shops (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    license_number VARCHAR(20) NOT NULL UNIQUE,
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    phone_number VARCHAR(20),
    email_address VARCHAR(255),
    website_url VARCHAR(500),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    logo_url VARCHAR(500),
    opening_hours VARCHAR(100),
    status ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_APPROVAL', 'REJECTED') DEFAULT 'PENDING_APPROVAL',
    is_featured BOOLEAN DEFAULT FALSE,
    subscription_plan VARCHAR(50),
    subscription_expiry TIMESTAMP NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_license_number (license_number),
    INDEX idx_status (status),
    INDEX idx_city (city),
    INDEX idx_state (state),
    INDEX idx_featured (is_featured)
);

-- Vehicles Table
CREATE TABLE vehicles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    brand_name VARCHAR(100),
    model_name VARCHAR(100),
    manufacturing_year INT,
    registration_number VARCHAR(20) UNIQUE,
    vehicle_type ENUM('CAR', 'BIKE', 'SCOOTER', 'TRUCK', 'BUS') NOT NULL,
    fuel_type VARCHAR(50),
    transmission_type VARCHAR(50),
    body_type VARCHAR(50),
    color VARCHAR(50),
    kilometers_driven INT,
    engine_capacity VARCHAR(20),
    price DECIMAL(12, 2) NOT NULL,
    is_negotiable BOOLEAN DEFAULT TRUE,
    condition VARCHAR(50),
    ownership_type VARCHAR(50),
    insurance_valid TIMESTAMP NULL,
    last_service_date TIMESTAMP NULL,
    main_image_url VARCHAR(500),
    video_url VARCHAR(500),
    specifications TEXT,
    features TEXT,
    view_count BIGINT DEFAULT 0,
    contact_count BIGINT DEFAULT 0,
    status ENUM('ACTIVE', 'INACTIVE', 'SOLD', 'PENDING_APPROVAL', 'REJECTED', 'FLAGGED') DEFAULT 'PENDING_APPROVAL',
    rejection_reason TEXT,
    is_featured BOOLEAN DEFAULT FALSE,
    shop_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    sold_at TIMESTAMP NULL,
    FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    INDEX idx_shop_id (shop_id),
    INDEX idx_category_id (category_id),
    INDEX idx_status (status),
    INDEX idx_vehicle_type (vehicle_type),
    INDEX idx_price (price),
    INDEX idx_year (manufacturing_year),
    INDEX idx_featured (is_featured),
    INDEX idx_registration_number (registration_number)
);

-- Vehicle Images Table (for multiple images)
CREATE TABLE vehicle_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    INDEX idx_vehicle_id (vehicle_id)
);

-- Enquiries Table
CREATE TABLE enquiries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(20),
    message TEXT,
    preferred_contact_method VARCHAR(50),
    budget_range VARCHAR(100),
    expected_purchase_time VARCHAR(100),
    financing_required BOOLEAN DEFAULT FALSE,
    test_drive_requested BOOLEAN DEFAULT FALSE,
    status ENUM('PENDING', 'CONTACTED', 'CLOSED', 'RESOLVED') DEFAULT 'PENDING',
    admin_notes TEXT,
    customer_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,
    INDEX idx_customer_id (customer_id),
    INDEX idx_vehicle_id (vehicle_id),
    INDEX idx_shop_id (shop_id),
    INDEX idx_status (status),
    INDEX idx_customer_email (customer_email)
);

-- Wishlist Table
CREATE TABLE wishlists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    UNIQUE KEY unique_customer_vehicle (customer_id, vehicle_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_vehicle_id (vehicle_id)
);

-- Insert Default Categories
INSERT INTO categories (name, description) VALUES
('Cars', 'Four-wheeled vehicles including sedans, SUVs, hatchbacks'),
('Bikes', 'Two-wheeled motorcycles'),
('Scooters', 'Two-wheeled scooters'),
('Trucks', 'Commercial trucks and pickups'),
('Buses', 'Commercial buses and vans');

-- Insert Default Super Admin (password: admin123)
INSERT INTO users (email, password, first_name, last_name, role, is_active, is_email_verified) VALUES
('admin@swarisewa.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Super', 'Admin', 'SUPER_ADMIN', TRUE, TRUE);

-- Create Views for Common Queries
CREATE VIEW active_vehicles AS
SELECT v.*, s.name as shop_name, s.city as shop_city, c.name as category_name
FROM vehicles v
JOIN shops s ON v.shop_id = s.id
JOIN categories c ON v.category_id = c.id
WHERE v.status = 'ACTIVE' AND s.status = 'ACTIVE';

CREATE VIEW shop_statistics AS
SELECT 
    s.id,
    s.name,
    s.city,
    s.state,
    COUNT(v.id) as total_vehicles,
    COUNT(CASE WHEN v.status = 'ACTIVE' THEN 1 END) as active_vehicles,
    COUNT(CASE WHEN v.status = 'SOLD' THEN 1 END) as sold_vehicles,
    COUNT(e.id) as total_enquiries,
    COUNT(CASE WHEN e.status = 'PENDING' THEN 1 END) as pending_enquiries,
    SUM(v.price) as total_inventory_value
FROM shops s
LEFT JOIN vehicles v ON s.id = v.shop_id
LEFT JOIN enquiries e ON s.id = e.shop_id
WHERE s.status = 'ACTIVE'
GROUP BY s.id, s.name, s.city, s.state;
