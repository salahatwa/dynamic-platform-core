-- Template Folder Management System - Complete Database Schema
-- PostgreSQL

-- 1. Enhanced Template Folders Table with hierarchical structure support
-- First, check if we need to modify existing table or create new one
DO $$
BEGIN
    -- Check if template_folders table exists and has the required columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'template_folders' AND column_name = 'application_id') THEN
        
        -- Drop existing table if it exists but doesn't have the right structure
        DROP TABLE IF EXISTS template_folders CASCADE;
        
        -- Create new table with complete structure
        CREATE TABLE template_folders (
            id BIGSERIAL PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            parent_id BIGINT NULL,
            application_id BIGINT NOT NULL,
            corporate_id BIGINT NOT NULL,
            path VARCHAR(1000) NOT NULL DEFAULT '', -- Materialized path for efficient queries
            level INTEGER NOT NULL DEFAULT 0,
            sort_order INTEGER NOT NULL DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            active BOOLEAN DEFAULT TRUE,
            
            FOREIGN KEY (parent_id) REFERENCES template_folders(id) ON DELETE CASCADE,
            FOREIGN KEY (application_id) REFERENCES apps(id) ON DELETE CASCADE,
            FOREIGN KEY (corporate_id) REFERENCES corporates(id) ON DELETE CASCADE,
            
            CONSTRAINT uk_name_parent_app UNIQUE (name, parent_id, application_id)
        );
    ELSE
        -- Table exists with application_id, ensure all columns are present
        ALTER TABLE template_folders 
        ADD COLUMN IF NOT EXISTS path VARCHAR(1000) DEFAULT '',
        ADD COLUMN IF NOT EXISTS level INTEGER DEFAULT 0,
        ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0,
        ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
        
        -- Update path column to NOT NULL after adding default values
        UPDATE template_folders SET path = '/' || id::text WHERE path IS NULL OR path = '';
        ALTER TABLE template_folders ALTER COLUMN path SET NOT NULL;
    END IF;
END $$;

-- Indexes for template_folders
CREATE INDEX IF NOT EXISTS idx_template_folders_parent_app ON template_folders(parent_id, application_id);
CREATE INDEX IF NOT EXISTS idx_template_folders_app_corporate ON template_folders(application_id, corporate_id);
CREATE INDEX IF NOT EXISTS idx_template_folders_path ON template_folders(path);
CREATE INDEX IF NOT EXISTS idx_template_folders_corporate_id ON template_folders(corporate_id);

-- 2. Add folder_id column to existing templates table if not exists
ALTER TABLE templates ADD COLUMN IF NOT EXISTS folder_id BIGINT NULL;

-- Add foreign key constraint for templates.folder_id
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_templates_folder_id' 
        AND table_name = 'templates'
    ) THEN
        ALTER TABLE templates ADD CONSTRAINT fk_templates_folder_id 
        FOREIGN KEY (folder_id) REFERENCES template_folders(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Add index for templates.folder_id
CREATE INDEX IF NOT EXISTS idx_templates_folder_id ON templates(folder_id);

-- 3. Folder Permissions removed - not needed for this implementation

-- 4. Create trigger function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_folder_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at columns
DROP TRIGGER IF EXISTS update_template_folders_updated_at ON template_folders;
CREATE TRIGGER update_template_folders_updated_at 
    BEFORE UPDATE ON template_folders
    FOR EACH ROW EXECUTE FUNCTION update_folder_updated_at_column();



-- 5. Function to update materialized path when folder hierarchy changes
CREATE OR REPLACE FUNCTION update_folder_path()
RETURNS TRIGGER AS $$
DECLARE
    parent_path VARCHAR(1000) := '';
    parent_level INTEGER := 0;
BEGIN
    -- Get parent path and level if parent exists
    IF NEW.parent_id IS NOT NULL THEN
        SELECT path, level INTO parent_path, parent_level
        FROM template_folders 
        WHERE id = NEW.parent_id;
        
        NEW.path := parent_path || '/' || NEW.id::text;
        NEW.level := parent_level + 1;
    ELSE
        NEW.path := '/' || NEW.id::text;
        NEW.level := 0;
    END IF;
    
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update path and level
DROP TRIGGER IF EXISTS update_folder_path_trigger ON template_folders;
CREATE TRIGGER update_folder_path_trigger
    BEFORE INSERT OR UPDATE OF parent_id ON template_folders
    FOR EACH ROW EXECUTE FUNCTION update_folder_path();

-- 6. Function to recursively update paths of all child folders
CREATE OR REPLACE FUNCTION update_child_folder_paths(folder_id BIGINT)
RETURNS VOID AS $$
DECLARE
    child_record RECORD;
BEGIN
    FOR child_record IN 
        SELECT id FROM template_folders WHERE parent_id = folder_id
    LOOP
        -- Update the child folder (trigger will handle path calculation)
        UPDATE template_folders 
        SET updated_at = CURRENT_TIMESTAMP 
        WHERE id = child_record.id;
        
        -- Recursively update grandchildren
        PERFORM update_child_folder_paths(child_record.id);
    END LOOP;
END;
$$ language 'plpgsql';

-- 7. Migration Script: Create root folders for existing applications and move templates
DO $$
DECLARE
    app_record RECORD;
    root_folder_id BIGINT;
    template_count INTEGER;
BEGIN
    -- Create root folders for each application that doesn't have one
    FOR app_record IN 
        SELECT DISTINCT a.id as app_id, a.corporate_id, a.name as app_name
        FROM apps a
        WHERE NOT EXISTS (
            SELECT 1 FROM template_folders tf 
            WHERE tf.application_id = a.id AND tf.parent_id IS NULL
        )
    LOOP
        -- Insert root folder for this application
        INSERT INTO template_folders (
            name, 
            parent_id, 
            application_id, 
            corporate_id, 
            path, 
            level, 
            sort_order,
            created_by, 
            updated_by
        ) VALUES (
            'Root', 
            NULL, 
            app_record.app_id, 
            app_record.corporate_id, 
            '', -- Will be updated by trigger
            0, 
            0,
            'SYSTEM_MIGRATION', 
            'SYSTEM_MIGRATION'
        ) RETURNING id INTO root_folder_id;
        
        -- Count templates that need to be moved
        SELECT COUNT(*) INTO template_count
        FROM templates t
        WHERE t.app_id = app_record.app_id 
        AND (t.folder_id IS NULL OR NOT EXISTS (
            SELECT 1 FROM template_folders tf WHERE tf.id = t.folder_id
        ));
        
        -- Move existing templates without valid folder_id to the root folder
        UPDATE templates 
        SET folder_id = root_folder_id,
            updated_at = CURRENT_TIMESTAMP
        WHERE app_id = app_record.app_id 
        AND (folder_id IS NULL OR NOT EXISTS (
            SELECT 1 FROM template_folders tf WHERE tf.id = templates.folder_id
        ));
        
        RAISE NOTICE 'Created root folder (ID: %) for application "%" and moved % templates', 
                     root_folder_id, app_record.app_name, template_count;
    END LOOP;
    
    -- Verify migration results
    SELECT COUNT(*) INTO template_count
    FROM templates t
    WHERE t.folder_id IS NULL;
    
    IF template_count > 0 THEN
        RAISE WARNING 'Migration incomplete: % templates still have NULL folder_id', template_count;
    ELSE
        RAISE NOTICE 'Migration completed successfully: All templates have been assigned to folders';
    END IF;
END $$;

-- 8. Create default permissions for root folders
DO $$
DECLARE
    folder_record RECORD;
    admin_role_id BIGINT;
    user_role_id BIGINT;
BEGIN
    -- Get role IDs (assuming standard role names)
    SELECT id INTO admin_role_id FROM roles WHERE name = 'ADMIN' LIMIT 1;
    SELECT id INTO user_role_id FROM roles WHERE name = 'USER' LIMIT 1;
    
    -- Only proceed if roles exist
    IF admin_role_id IS NOT NULL THEN
        -- Grant full permissions to admin role for all root folders
        FOR folder_record IN 
            SELECT id FROM template_folders WHERE parent_id IS NULL
        LOOP
            INSERT INTO folder_permissions (
                folder_id, role_id, can_view, can_create, can_edit, can_delete
            ) VALUES (
                folder_record.id, admin_role_id, TRUE, TRUE, TRUE, TRUE
            ) ON CONFLICT (folder_id, role_id) DO NOTHING;
        END LOOP;
        
        RAISE NOTICE 'Granted admin permissions to % root folders', 
                     (SELECT COUNT(*) FROM template_folders WHERE parent_id IS NULL);
    END IF;
    
    -- Grant view and create permissions to user role if it exists
    IF user_role_id IS NOT NULL THEN
        FOR folder_record IN 
            SELECT id FROM template_folders WHERE parent_id IS NULL
        LOOP
            INSERT INTO folder_permissions (
                folder_id, role_id, can_view, can_create, can_edit, can_delete
            ) VALUES (
                folder_record.id, user_role_id, TRUE, TRUE, FALSE, FALSE
            ) ON CONFLICT (folder_id, role_id) DO NOTHING;
        END LOOP;
        
        RAISE NOTICE 'Granted user permissions to % root folders', 
                     (SELECT COUNT(*) FROM template_folders WHERE parent_id IS NULL);
    END IF;
END $$;

-- 9. Validation queries to verify migration success
DO $$
DECLARE
    orphaned_templates INTEGER;
    folders_without_path INTEGER;
    total_folders INTEGER;
    total_templates INTEGER;
BEGIN
    -- Check for orphaned templates
    SELECT COUNT(*) INTO orphaned_templates
    FROM templates t
    WHERE t.folder_id IS NOT NULL 
    AND NOT EXISTS (SELECT 1 FROM template_folders tf WHERE tf.id = t.folder_id);
    
    -- Check for folders without proper path
    SELECT COUNT(*) INTO folders_without_path
    FROM template_folders
    WHERE path IS NULL OR path = '';
    
    -- Get totals
    SELECT COUNT(*) INTO total_folders FROM template_folders;
    SELECT COUNT(*) INTO total_templates FROM templates;
    
    -- Report results
    RAISE NOTICE '=== MIGRATION VALIDATION RESULTS ===';
    RAISE NOTICE 'Total folders created: %', total_folders;
    RAISE NOTICE 'Total templates: %', total_templates;
    RAISE NOTICE 'Orphaned templates: %', orphaned_templates;
    RAISE NOTICE 'Folders without path: %', folders_without_path;
    
    IF orphaned_templates = 0 AND folders_without_path = 0 THEN
        RAISE NOTICE 'SUCCESS: Migration completed without errors';
    ELSE
        RAISE WARNING 'ISSUES DETECTED: Please review migration results';
    END IF;
END $$;

-- 10. Create helpful views for folder management
CREATE OR REPLACE VIEW folder_tree_view AS
SELECT 
    tf.id,
    tf.name,
    tf.parent_id,
    tf.application_id,
    tf.corporate_id,
    tf.path,
    tf.level,
    tf.sort_order,
    tf.created_at,
    tf.updated_at,
    a.name as application_name,
    c.name as corporate_name,
    (SELECT COUNT(*) FROM templates t WHERE t.folder_id = tf.id) as template_count,
    (SELECT COUNT(*) FROM template_folders child WHERE child.parent_id = tf.id) as subfolder_count
FROM template_folders tf
JOIN apps a ON tf.application_id = a.id
JOIN corporates c ON tf.corporate_id = c.id;

-- View for templates with folder information
CREATE OR REPLACE VIEW templates_with_folder_view AS
SELECT 
    t.*,
    tf.name as folder_name,
    tf.path as folder_path,
    tf.level as folder_level,
    a.name as application_name,
    c.name as corporate_name
FROM templates t
LEFT JOIN template_folders tf ON t.folder_id = tf.id
LEFT JOIN apps a ON t.app_id = a.id
LEFT JOIN corporates c ON t.corporate_id = c.id;

COMMIT;