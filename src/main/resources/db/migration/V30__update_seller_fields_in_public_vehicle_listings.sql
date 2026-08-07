ALTER TABLE public_vehicle_listings
  ADD COLUMN seller_address TEXT NULL AFTER seller_phone,
  DROP COLUMN seller_email,
  DROP COLUMN seller_city,
  DROP COLUMN seller_area,
  DROP COLUMN preferred_contact_method;
