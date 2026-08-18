-- Public users sign up with mobile number only. The email column is no
-- longer required for customer accounts, so make it nullable. Existing
-- synthetic emails (e.g. 9779...@mobile.swarisadhan.com) are cleared so
-- they don't leak into admin views or duplicate-account checks.
ALTER TABLE users MODIFY COLUMN email VARCHAR(255) NULL;

-- Clear previously-generated synthetic emails so they don't show up anywhere.
UPDATE users SET email = NULL WHERE email LIKE '%@mobile.swarisadhan.com';
