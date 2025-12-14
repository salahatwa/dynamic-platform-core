-- Template Editor Enhancement - Database Schema
-- PostgreSQL

-- 1. Template Folders Table
CREATE TABLE IF NOT EXISTS template_folders (
    id BIGSERIAL PRIMARY KEY,
    corporate_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    parent_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (corporate_id) REFERENCES corporates(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES template_folders(id) ON DELETE CASCADE,
    UNIQUE (corporate_id, name, parent_id)
);

CREATE INDEX IF NOT EXISTS idx_template_folders_corporate_id ON template_folders(corporate_id);
CREATE INDEX IF NOT EXISTS idx_template_folders_parent_id ON template_folders(parent_id);

-- 2. Enhance Templates Table
ALTER TABLE templates ADD COLUMN IF NOT EXISTS template_type VARCHAR(20) DEFAULT 'HTML';
ALTER TABLE templates ADD COLUMN IF NOT EXISTS folder_id BIGINT;
ALTER TABLE templates ADD CONSTRAINT fk_templates_folder FOREIGN KEY (folder_id) REFERENCES template_folders(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_templates_folder_id ON templates(folder_id);
CREATE INDEX IF NOT EXISTS idx_templates_type ON templates(template_type);

-- 3. Template Pages Table (for multi-page HTML templates)
CREATE TABLE IF NOT EXISTS template_pages (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    content TEXT,
    page_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES templates(id) ON DELETE CASCADE,
    UNIQUE (template_id, page_order)
);

CREATE INDEX IF NOT EXISTS idx_template_pages_template_id ON template_pages(template_id);
CREATE INDEX IF NOT EXISTS idx_template_pages_order ON template_pages(template_id, page_order);

-- 4. Template Attributes Table (dynamic key-value attributes)
CREATE TABLE IF NOT EXISTS template_attributes (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    attribute_key VARCHAR(255) NOT NULL,
    attribute_value TEXT,
    attribute_type VARCHAR(50) DEFAULT 'STRING',
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES templates(id) ON DELETE CASCADE,
    UNIQUE (template_id, attribute_key)
);

CREATE INDEX IF NOT EXISTS idx_template_attributes_template_id ON template_attributes(template_id);
CREATE INDEX IF NOT EXISTS idx_template_attributes_key ON template_attributes(attribute_key);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_template_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_template_folders_updated_at BEFORE UPDATE ON template_folders
    FOR EACH ROW EXECUTE FUNCTION update_template_updated_at_column();

CREATE TRIGGER update_template_pages_updated_at BEFORE UPDATE ON template_pages
    FOR EACH ROW EXECUTE FUNCTION update_template_updated_at_column();

CREATE TRIGGER update_template_attributes_updated_at BEFORE UPDATE ON template_attributes
    FOR EACH ROW EXECUTE FUNCTION update_template_updated_at_column();

-- Insert default folder for existing templates
INSERT INTO template_folders (corporate_id, name, parent_id)
SELECT DISTINCT corporate_id, 'Default', NULL
FROM templates
WHERE NOT EXISTS (
    SELECT 1 FROM template_folders WHERE name = 'Default'
)
ON CONFLICT DO NOTHING;
