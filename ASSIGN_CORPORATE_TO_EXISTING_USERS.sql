-- Script to assign corporate to existing users who don't have one
-- This handles users created before the automated corporate registration was implemented

-- Step 1: Check users without corporate
SELECT id, email, name, corporate_id 
FROM users 
WHERE corporate_id IS NULL;

-- Step 2: For each user without corporate, create one and assign it
-- Example for a specific user (replace user_id with actual ID):

-- Create corporate for user
INSERT INTO corporates (name, domain, description, created_at, updated_at)
VALUES 
    ('User Name''s Organization', 'username', 'Auto-created organization', NOW(), NOW());

-- Get the corporate ID (or use LAST_INSERT_ID() in MySQL)
SET @corporate_id = LAST_INSERT_ID();

-- Assign corporate to user
UPDATE users 
SET corporate_id = @corporate_id 
WHERE id = 3; -- Replace with actual user ID

-- Assign ADMIN role to user (if not already assigned)
INSERT INTO user_roles (user_id, role_id)
SELECT 3, r.id 
FROM roles r 
WHERE r.name = 'ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = 3 AND ur.role_id = r.id
);

-- Step 3: Verify the changes
SELECT u.id, u.email, u.name, c.name as corporate_name, c.domain, r.name as role_name
FROM users u
LEFT JOIN corporates c ON u.corporate_id = c.id
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.id = 3; -- Replace with actual user ID
