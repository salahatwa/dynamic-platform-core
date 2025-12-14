-- Assign default role to user for API key authentication
-- Run this SQL to give your user basic access

-- First, check if ADMIN role exists, if not create it
INSERT INTO roles (name, description, active, created_at, updated_at)
SELECT 'ADMIN', 'Administrator role with full access', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');

-- Assign ADMIN role to your user (replace email if different)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'yassen@gmail.com'
  AND r.name = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

-- Verify the assignment
SELECT u.email, r.name as role_name
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.email = 'yassen@gmail.com';
