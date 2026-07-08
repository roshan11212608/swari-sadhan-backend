USE swarisadhan;

-- Delete existing user
DELETE FROM users WHERE email='admin@swarisewa.com';

-- Insert user with correct password hash
INSERT INTO users (email, password, first_name, last_name, role, is_active, is_email_verified, created_at, updated_at) 
VALUES ('admin@swarisewa.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Super', 'Admin', 'SUPERADMIN', TRUE, TRUE, NOW(), NOW());

-- Verify the insertion
SELECT email, LENGTH(password) as password_length, password FROM users WHERE email='admin@swarisewa.com';
