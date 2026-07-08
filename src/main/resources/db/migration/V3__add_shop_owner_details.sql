-- Add new columns to shop_owners table for comprehensive shop owner management
-- Migration script for ShopOwner entity updates, made idempotent to avoid duplicate column errors

DROP PROCEDURE IF EXISTS AddColumnsToShopOwners;
DELIMITER //
CREATE PROCEDURE AddColumnsToShopOwners()
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO col_exists 
    FROM information_schema.columns 
    WHERE table_schema = DATABASE()
      AND table_name = 'shop_owners' 
      AND column_name = 'father_name';
      
    IF col_exists = 0 THEN
        ALTER TABLE shop_owners 
        ADD COLUMN father_name VARCHAR(255),
        ADD COLUMN profile_photo TEXT,
        ADD COLUMN citizenship_no VARCHAR(100),
        ADD COLUMN citizenship_pic TEXT,
        ADD COLUMN shop_name VARCHAR(255),
        ADD COLUMN shop_type VARCHAR(100),
        ADD COLUMN province VARCHAR(100),
        ADD COLUMN district VARCHAR(100),
        ADD COLUMN municipality VARCHAR(100),
        ADD COLUMN ward VARCHAR(50),
        ADD COLUMN tole VARCHAR(255),
        ADD COLUMN shop_phone VARCHAR(20),
        ADD COLUMN shop_email VARCHAR(255),
        ADD COLUMN shop_logo TEXT,
        ADD COLUMN pan VARCHAR(50),
        ADD COLUMN reg_cert VARCHAR(100),
        ADD COLUMN vat VARCHAR(50),
        ADD COLUMN opening_time TIME,
        ADD COLUMN closing_time TIME,
        ADD COLUMN off_days VARCHAR(255),
        ADD COLUMN subscription_start_date VARCHAR(50),
        ADD COLUMN subscription_expiry_date VARCHAR(50),
        ADD COLUMN vehicle_limit INT DEFAULT 5,
        ADD COLUMN staff_limit INT DEFAULT 3,
        ADD COLUMN citizenship_upload TEXT,
        ADD COLUMN shop_reg_upload TEXT,
        ADD COLUMN whatsapp_no VARCHAR(20),
        ADD COLUMN facebook_page VARCHAR(500),
        ADD COLUMN google_map_link TEXT,
        ADD COLUMN notes TEXT;
        
        -- Add indexes for frequently searched columns
        CREATE INDEX idx_shop_owners_shop_name ON shop_owners(shop_name);
        CREATE INDEX idx_shop_owners_shop_type ON shop_owners(shop_type);
        CREATE INDEX idx_shop_owners_province ON shop_owners(province);
        CREATE INDEX idx_shop_owners_district ON shop_owners(district);
        CREATE INDEX idx_shop_owners_pan ON shop_owners(pan);
        CREATE INDEX idx_shop_owners_subscription_plan ON shop_owners(subscription_plan);
    END IF;
END //
DELIMITER ;

CALL AddColumnsToShopOwners();
DROP PROCEDURE AddColumnsToShopOwners;

-- Update existing records to have default values for non-nullable fields (safe to run)
UPDATE shop_owners 
SET vehicle_limit = 5, 
    staff_limit = 3,
    subscription_active = true
WHERE vehicle_limit IS NULL OR staff_limit IS NULL;
