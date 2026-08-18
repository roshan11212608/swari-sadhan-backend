-- Shop owner approval workflow: registrations are now pending until superadmin
-- approves them. On approval, a temp password is generated and emailed.
ALTER TABLE shop_owners ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE shop_owners ADD COLUMN password_changed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE shop_owners ADD COLUMN rejection_reason TEXT NULL;
ALTER TABLE shop_owners ADD COLUMN approved_at DATETIME NULL;
ALTER TABLE shop_owners ADD COLUMN approved_by BIGINT NULL;

-- Existing shop owners are treated as already approved.
UPDATE shop_owners SET approval_status = 'APPROVED', password_changed = TRUE WHERE active = TRUE;
