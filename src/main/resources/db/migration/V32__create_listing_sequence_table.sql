CREATE TABLE IF NOT EXISTS public_vehicle_listing_sequence (
    period_key VARCHAR(6) NOT NULL PRIMARY KEY,
    next_value BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Drop the redundant non-unique index on listing_number (the UNIQUE constraint already covers lookups)
ALTER TABLE public_vehicle_listings DROP INDEX idx_pvl_listing_number;
