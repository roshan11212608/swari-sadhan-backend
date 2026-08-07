ALTER TABLE public_vehicle_listings ADD COLUMN owner_phone VARCHAR(20) NOT NULL DEFAULT '' AFTER owner_name;
ALTER TABLE public_vehicle_listings MODIFY COLUMN owner_phone VARCHAR(20) NOT NULL;
