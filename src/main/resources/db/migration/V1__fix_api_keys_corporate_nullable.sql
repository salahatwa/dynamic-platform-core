-- Make corporate_id nullable in api_keys table
ALTER TABLE api_keys ALTER COLUMN corporate_id DROP NOT NULL;
