-- Update LOV table structure
ALTER TABLE lov RENAME COLUMN value TO lov_value;

-- Add new columns
ALTER TABLE lov ADD COLUMN IF NOT EXISTS attribute1 VARCHAR(200);
ALTER TABLE lov ADD COLUMN IF NOT EXISTS attribute2 VARCHAR(200);
ALTER TABLE lov ADD COLUMN IF NOT EXISTS attribute3 VARCHAR(200);
ALTER TABLE lov ADD COLUMN IF NOT EXISTS translation_app VARCHAR(100);

-- Update existing data to have translation_app
UPDATE lov SET translation_app = 'default' WHERE translation_app IS NULL;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_lov_translation ON lov(translation_app, translation_key);
CREATE INDEX IF NOT EXISTS idx_lov_value ON lov(lov_value);

-- Update sample data with new structure
UPDATE lov SET 
    lov_value = '1',
    attribute1 = 'US',
    attribute2 = 'USA',
    attribute3 = '+1',
    translation_app = 'default'
WHERE lov_code = 'COUNTRY_US';

UPDATE lov SET 
    lov_value = '2',
    attribute1 = 'UK',
    attribute2 = 'GBR',
    attribute3 = '+44',
    translation_app = 'default'
WHERE lov_code = 'COUNTRY_UK';

UPDATE lov SET 
    lov_value = '3',
    attribute1 = 'SA',
    attribute2 = 'SAU',
    attribute3 = '+966',
    translation_app = 'default'
WHERE lov_code = 'COUNTRY_SA';

-- Comments
COMMENT ON COLUMN lov.lov_value IS 'Numeric or text value for the LOV';
COMMENT ON COLUMN lov.attribute1 IS 'Flexible attribute 1';
COMMENT ON COLUMN lov.attribute2 IS 'Flexible attribute 2';
COMMENT ON COLUMN lov.attribute3 IS 'Flexible attribute 3';
COMMENT ON COLUMN lov.translation_app IS 'Translation application name';
