-- Seed default admin user
-- Email:    admin@example.com
-- Password: admin123
-- The hash is bcrypt with cost factor 12 (matches BCryptPasswordEncoder(12) in SecurityConfig).
-- Spring Security accepts the $2y$ variant.
-- IMPORTANT: rotate this password in production environments.

INSERT INTO users (id, email, password_hash, first_name, last_name, enabled, account_locked, failed_login_attempts)
VALUES (
    gen_random_uuid(),
    'admin@example.com',
    '$2y$12$7Wb9nRnu2LefIuD.SyV96.RHZQq4c43Hib51bUEBwi55b/P8d426G',
    'Platform',
    'Administrator',
    TRUE,
    FALSE,
    0
)
ON CONFLICT (email) DO NOTHING;

-- Link the admin user to ROLE_ADMIN
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.email = 'admin@example.com'
  AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
