-- Make value column nullable in lov_version table
-- This allows LOV type entries (which don't have values) to be versioned

ALTER TABLE lov_version ALTER COLUMN value DROP NOT NULL;
