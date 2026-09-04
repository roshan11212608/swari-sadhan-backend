-- V65: Add display_order column to shops for admin-controlled showroom ordering
-- on the public /showrooms page.  Existing shops get a default order based on
-- their creation date so the initial listing is deterministic.

ALTER TABLE shops ADD COLUMN display_order INT NOT NULL DEFAULT 0;

-- Backfill: order by created_at so oldest shops appear first
SET @row = 0;
UPDATE shops SET display_order = (@row := @row + 1) ORDER BY created_at ASC;

-- Index for efficient ordering on the public listing
CREATE INDEX idx_shops_display_order ON shops (display_order);
