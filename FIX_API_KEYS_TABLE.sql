-- Fix api_keys table constraints
-- Run this SQL script directly in your PostgreSQL database

-- Make corporate_id nullable
ALTER TABLE api_keys ALTER COLUMN corporate_id DROP NOT NULL;

-- Make created_by nullable (if column exists)
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'api_keys' AND column_name = 'created_by'
    ) THEN
        ALTER TABLE api_keys ALTER COLUMN created_by DROP NOT NULL;
    END IF;
END $$;

-- Make updated_by nullable (if column exists)
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'api_keys' AND column_name = 'updated_by'
    ) THEN
        ALTER TABLE api_keys ALTER COLUMN updated_by DROP NOT NULL;
    END IF;
END $$;

-- Verify the changes
SELECT 
    column_name, 
    is_nullable, 
    data_type 
FROM information_schema.columns 
WHERE table_name = 'api_keys' 
  AND column_name IN ('corporate_id', 'created_by', 'updated_by')
ORDER BY column_name;

-- Expected result: is_nullable should be 'YES' for all columns
