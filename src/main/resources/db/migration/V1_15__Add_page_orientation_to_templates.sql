-- Add page orientation column to templates table
ALTER TABLE templates ADD COLUMN page_orientation VARCHAR(20) DEFAULT 'PORTRAIT';

-- Update existing templates to have PORTRAIT orientation by default
UPDATE templates SET page_orientation = 'PORTRAIT' WHERE page_orientation IS NULL;