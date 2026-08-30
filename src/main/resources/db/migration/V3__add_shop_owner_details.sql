-- Add new columns to shop_owners table for comprehensive shop owner management
-- TiDB-compatible version using IF NOT EXISTS instead of stored procedures

ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS father_name VARCHAR(255);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS profile_photo TEXT;
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS citizenship_no VARCHAR(100);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS citizenship_pic TEXT;
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS shop_name VARCHAR(255);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS shop_type VARCHAR(100);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS province VARCHAR(100);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS district VARCHAR(100);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS municipality VARCHAR(100);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS ward VARCHAR(50);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS tole VARCHAR(255);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS shop_phone VARCHAR(20);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS shop_email VARCHAR(255);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS shop_logo TEXT;
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS pan VARCHAR(50);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS reg_cert VARCHAR(100);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS vat VARCHAR(50);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS opening_time TIME;
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS closing_time TIME;
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS off_days VARCHAR(255);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS subscription_start_date VARCHAR(50);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS subscription_expiry_date VARCHAR(50);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS vehicle_limit INT DEFAULT 5;
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS staff_limit INT DEFAULT 3;
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS citizenship_upload TEXT;
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS shop_reg_upload TEXT;
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS whatsapp_no VARCHAR(20);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS facebook_page VARCHAR(500);
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS google_map_link TEXT;
ALTER TABLE shop_owners ADD COLUMN IF NOT EXISTS notes TEXT;

-- Add indexes for frequently searched columns
CREATE INDEX IF NOT EXISTS idx_shop_owners_shop_name ON shop_owners(shop_name);
CREATE INDEX IF NOT EXISTS idx_shop_owners_shop_type ON shop_owners(shop_type);
CREATE INDEX IF NOT EXISTS idx_shop_owners_province ON shop_owners(province);
CREATE INDEX IF NOT EXISTS idx_shop_owners_district ON shop_owners(district);
CREATE INDEX IF NOT EXISTS idx_shop_owners_pan ON shop_owners(pan);
CREATE INDEX IF NOT EXISTS idx_shop_owners_subscription_plan ON shop_owners(subscription_plan);

-- Update existing records to have default values for non-nullable fields (safe to run)
UPDATE shop_owners
SET vehicle_limit = 5,
    staff_limit = 3,
    subscription_active = true
WHERE vehicle_limit IS NULL OR staff_limit IS NULL;
